package com.xinke.datahub.naming;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EnglishNameGenerator
{
    private static final Pattern CJK = Pattern.compile("[\\p{IsHan}]");
    private static final Set<String> RESERVED = Set.of(
            "add", "all", "alter", "and", "as", "asc", "between", "by", "case", "column", "create",
            "database", "date", "decimal", "default", "delete", "desc", "distinct", "drop", "else", "exists",
            "from", "group", "having", "in", "index", "insert", "int", "into", "is", "join", "key", "like",
            "limit", "long", "not", "null", "on", "or", "order", "primary", "references", "select", "set",
            "table", "text", "then", "time", "timestamp", "union", "unique", "update", "user", "values",
            "varchar", "when", "where");

    private static final Map<String, String> BUILTIN = builtInDictionary();

    public GeneratedName generate(String source, String fallbackPrefix, int fallbackIndex, Map<String, String> overrides)
    {
        String normalized = Normalizer.normalize(source == null ? "" : source.strip(), Normalizer.Form.NFKC);
        Map<String, String> dictionary = new HashMap<>(BUILTIN);
        if (overrides != null) dictionary.putAll(overrides);

        String exact = dictionary.get(normalized);
        if (exact != null)
        {
            String identifier = normalizeIdentifier(exact);
            return new GeneratedName(avoidReserved(identifier), "DICTIONARY", false);
        }

        boolean hadChinese = CJK.matcher(normalized).find();
        boolean unresolved = false;
        String translated = normalized.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        if (hadChinese)
        {
            List<Map.Entry<String, String>> entries = new ArrayList<>(dictionary.entrySet());
            entries.sort(Comparator.<Map.Entry<String, String>>comparingInt(e -> e.getKey().length()).reversed());
            for (Map.Entry<String, String> entry : entries)
            {
                if (translated.contains(entry.getKey()))
                    translated = translated.replace(entry.getKey(), "_" + entry.getValue() + "_");
            }
            unresolved = CJK.matcher(translated).find();
            translated = translated.replaceAll("[\\p{IsHan}]+", "_");
        }

        String identifier = normalizeIdentifier(translated);
        if (identifier.isBlank())
        {
            identifier = fallbackPrefix + "_" + String.format(Locale.ROOT, "%03d", fallbackIndex);
            unresolved = true;
        }
        if (Character.isDigit(identifier.charAt(0))) identifier = fallbackPrefix + "_" + identifier;
        if (identifier.length() > 48) identifier = identifier.substring(0, 48).replaceAll("_+$", "");
        identifier = avoidReserved(identifier);
        return new GeneratedName(identifier, unresolved ? "FALLBACK" : hadChinese ? "COMPOSITE" : "NORMALIZED", unresolved);
    }

    public String normalizeNameKey(String displayName)
    {
        String value = Normalizer.normalize(displayName == null ? "" : displayName, Normalizer.Form.NFKC);
        return value.strip().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    public String normalizeIdentifier(String value)
    {
        String identifier = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "").replaceAll("_+", "_");
        return identifier;
    }

    private String avoidReserved(String identifier)
    {
        return RESERVED.contains(identifier) || identifier.startsWith("_") ? "field_" + identifier : identifier;
    }

    private static Map<String, String> builtInDictionary()
    {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("客户订单", "customer_order");
        values.put("采购订单", "purchase_order");
        values.put("销售订单", "sales_order");
        values.put("商品明细", "product_detail");
        values.put("订单编号", "order_no");
        values.put("订单号", "order_no");
        values.put("客户名称", "customer_name");
        values.put("客户姓名", "customer_name");
        values.put("商品名称", "product_name");
        values.put("供应商名称", "supplier_name");
        values.put("下单时间", "order_time");
        values.put("创建时间", "create_time");
        values.put("更新时间", "update_time");
        values.put("含税金额", "tax_included_amount");
        values.put("联系电话", "contact_phone");
        values.put("手机号码", "mobile_phone");
        values.put("身份证号", "id_card_no");
        values.put("客户", "customer");
        values.put("订单", "order");
        values.put("采购", "purchase");
        values.put("销售", "sales");
        values.put("商品", "product");
        values.put("产品", "product");
        values.put("供应商", "supplier");
        values.put("仓库", "warehouse");
        values.put("店铺", "shop");
        values.put("名称", "name");
        values.put("姓名", "name");
        values.put("编号", "no");
        values.put("编码", "code");
        values.put("序号", "sequence_no");
        values.put("日期", "date");
        values.put("时间", "time");
        values.put("金额", "amount");
        values.put("价格", "price");
        values.put("单价", "unit_price");
        values.put("数量", "quantity");
        values.put("状态", "status");
        values.put("备注", "remark");
        values.put("地址", "address");
        values.put("电话", "phone");
        values.put("手机", "mobile");
        values.put("邮箱", "email");
        values.put("类型", "type");
        values.put("分类", "category");
        values.put("税率", "tax_rate");
        values.put("税额", "tax_amount");
        values.put("成本", "cost");
        values.put("利润", "profit");
        values.put("重量", "weight");
        values.put("单位", "unit");
        return values;
    }
}
