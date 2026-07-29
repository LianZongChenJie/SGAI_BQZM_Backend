package org.jeecg.modules.bems.energyAnalysis.util;

import cn.hutool.core.collection.CollectionUtil;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jexl3Util {

    public static BigDecimal getValue(String formula,Map<String,BigDecimal> codeValue){
        Set<String> pointCode = getCode(formula);
        if(CollectionUtil.isEmpty(pointCode)){
            return new BigDecimal(formula);
        }
        int i = 0;
        Map<String,BigDecimal> formulaCodeValue = new HashMap<>();
        for (String item : pointCode) {
            BigDecimal value = codeValue.getOrDefault(item, BigDecimal.ZERO);
            if(formula.contains("["+item+"]")){
                String code = "A" + i;
                formulaCodeValue.put(code,value);
                int j = 0;
                while(formula.contains("[" + item + "]")){
                    formula = formula.replace("[" + item + "]", code);
                    j++;
                    if(j > 500){
                        break;
                    }
                }
                i++;
            }
        }

        Map<String, Object> namespaces = new HashMap<>();
        JexlExpression js = new JexlBuilder().namespaces(namespaces).create().createExpression(formula);
        JexlContext context = new MapContext();
        formulaCodeValue.forEach(context::set);
        Object execute = js.evaluate(context);
        // 对结构进行转换
        if (execute instanceof String) {
            return new BigDecimal((String) execute);
        } else if (execute instanceof BigDecimal) {
            return (BigDecimal) execute;
        }else{
            return new BigDecimal(execute.toString());
        }
    }

    private static Set<String> getCode(String formula) {
        // 正则表达式，用于匹配方括号内的内容
        String regex = "\\[([\\s\\S]*?)\\]";
        Pattern pattern = Pattern.compile(regex);
        Set<String> result = new HashSet<>();
        Matcher matcher = pattern.matcher(formula);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
