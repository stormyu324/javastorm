package com.javastorm.shop.service;

import com.javastorm.shop.domain.Category;
import com.javastorm.shop.domain.Product;
import com.javastorm.shop.domain.ProductStatus;
import com.javastorm.shop.repository.CategoryRepository;
import com.javastorm.shop.repository.ProductRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 通过 CSV（可用 Excel 编辑后“另存为 CSV”）批量上架商品。
 * 以 SKU 为准：已存在的 SKU 会被更新，新的 SKU 会新建；未填写 SKU 时自动生成。
 */
@Service
public class ProductImportService {

    /** 模板表头，同时接受这些列的英文别名。 */
    public static final List<String> HEADERS = List.of("名称", "SKU", "分类", "售价", "原价", "库存", "状态", "图片", "描述");

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("name", "名称"), Map.entry("商品名称", "名称"),
            Map.entry("sku", "SKU"), Map.entry("编码", "SKU"), Map.entry("商品编码", "SKU"),
            Map.entry("category", "分类"),
            Map.entry("price", "售价"), Map.entry("价格", "售价"),
            Map.entry("originalprice", "原价"), Map.entry("original_price", "原价"),
            Map.entry("stock", "库存"),
            Map.entry("status", "状态"),
            Map.entry("images", "图片"), Map.entry("image", "图片"), Map.entry("图片地址", "图片"),
            Map.entry("description", "描述"), Map.entry("商品描述", "描述"));

    private static final Charset GB18030 = Charset.forName("GB18030");

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductService productService;

    public ProductImportService(ProductRepository products, CategoryRepository categories, ProductService productService) {
        this.products = products;
        this.categories = categories;
        this.productService = productService;
    }

    public record RowError(long line, String message) {
    }

    public record ImportResult(int created, int updated, List<RowError> errors) {
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    /**
     * @param bytes         CSV 文件内容，自动识别 UTF-8 / GBK 编码
     * @param defaultStatus 行内未填写“状态”时使用的状态
     */
    @Transactional
    public ImportResult importCsv(byte[] bytes, ProductStatus defaultStatus) {
        String text = decode(bytes);
        if (text.startsWith("﻿")) {
            text = text.substring(1);
        }
        int created = 0;
        int updated = 0;
        List<RowError> errors = new ArrayList<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        try (CSVParser parser = CSVParser.parse(new StringReader(text), format)) {
            Map<String, String> columns = resolveColumns(parser.getHeaderNames());
            if (!columns.containsKey("名称") || !columns.containsKey("售价")) {
                throw new BusinessException("CSV 表头至少需要包含「名称」和「售价」两列，请下载模板对照");
            }
            for (CSVRecord record : parser) {
                long line = record.getRecordNumber() + 1; // +1 是表头行
                try {
                    boolean isNew = importRow(record, columns, defaultStatus);
                    if (isNew) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (BusinessException | IllegalArgumentException e) {
                    errors.add(new RowError(line, e.getMessage()));
                }
            }
        } catch (IOException | IllegalStateException e) {
            throw new BusinessException("无法解析 CSV 文件：" + e.getMessage());
        }
        return new ImportResult(created, updated, errors);
    }

    /** 带 BOM 的 UTF-8 模板，Excel 打开不会乱码。 */
    public byte[] template() {
        String csv = "﻿" + String.join(",", HEADERS) + "\r\n"
                + "纯棉圆领T恤 白色 M码,TSHIRT-WHITE-M,服装,59.00,99.00,100,上架,https://example.com/a.jpg|https://example.com/b.jpg,100% 纯棉，透气舒适\r\n"
                + "陶瓷马克杯,,家居,35,,50,,,\r\n";
        return csv.getBytes(StandardCharsets.UTF_8);
    }

    private boolean importRow(CSVRecord record, Map<String, String> columns, ProductStatus defaultStatus) {
        String name = value(record, columns, "名称");
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("名称不能为空");
        }
        if (name.length() > 200) {
            throw new BusinessException("名称超过 200 字");
        }
        BigDecimal price = decimal(value(record, columns, "售价"), "售价");
        if (price == null) {
            throw new BusinessException("售价不能为空");
        }
        BigDecimal originalPrice = decimal(value(record, columns, "原价"), "原价");
        String stockText = value(record, columns, "库存");
        Integer stock = null;
        if (StringUtils.hasText(stockText)) {
            try {
                stock = Integer.parseInt(stockText);
            } catch (NumberFormatException e) {
                throw new BusinessException("库存「" + stockText + "」不是整数");
            }
            if (stock < 0) {
                throw new BusinessException("库存不能为负数");
            }
        }
        ProductStatus status = ProductStatus.parse(value(record, columns, "状态"));

        String sku = value(record, columns, "SKU");
        Product product = StringUtils.hasText(sku) ? products.findBySku(sku).orElse(null) : null;
        boolean isNew = product == null;
        if (isNew) {
            product = new Product();
            product.setSku(StringUtils.hasText(sku) ? sku : productService.generateSku());
            product.setStatus(defaultStatus);
            product.setStock(0);
        }
        product.setName(name);
        product.setPrice(price);
        if (originalPrice != null || isNew) {
            product.setOriginalPrice(originalPrice);
        }
        if (stock != null) {
            product.setStock(stock);
        }
        if (status != null) {
            product.setStatus(status);
        }
        String categoryName = value(record, columns, "分类");
        if (StringUtils.hasText(categoryName)) {
            product.setCategory(categories.findByName(categoryName)
                    .orElseGet(() -> categories.save(new Category(categoryName))));
        }
        String imagesText = value(record, columns, "图片");
        if (StringUtils.hasText(imagesText)) {
            product.setImageUrls(Arrays.stream(imagesText.split("[|\\n]"))
                    .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList());
        }
        String description = value(record, columns, "描述");
        if (description != null && (!description.isEmpty() || isNew)) {
            product.setDescription(description);
        }
        products.save(product);
        return isNew;
    }

    private static Map<String, String> resolveColumns(List<String> headerNames) {
        Map<String, String> columns = new HashMap<>();
        for (String raw : headerNames) {
            String h = raw.trim();
            String key = HEADERS.contains(h) ? h
                    : HEADERS.stream().filter(x -> x.equalsIgnoreCase(h)).findFirst()
                    .orElse(ALIASES.get(h.toLowerCase(Locale.ROOT)));
            if (key != null) {
                columns.putIfAbsent(key, raw);
            }
        }
        return columns;
    }

    private static String value(CSVRecord record, Map<String, String> columns, String key) {
        String column = columns.get(key);
        if (column == null || !record.isSet(column)) {
            return null;
        }
        return record.get(column).trim();
    }

    private static BigDecimal decimal(String text, String field) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(text.replace("¥", "").replace("￥", "").replace(",", "").trim());
            if (v.signum() < 0) {
                throw new BusinessException(field + "不能为负数");
            }
            return v.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new BusinessException(field + "「" + text + "」不是有效数字");
        }
    }

    /** 优先按 UTF-8 解码；失败时按 GBK/GB18030（中文版 Excel 另存 CSV 的默认编码）。 */
    static String decode(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, GB18030);
        }
    }
}
