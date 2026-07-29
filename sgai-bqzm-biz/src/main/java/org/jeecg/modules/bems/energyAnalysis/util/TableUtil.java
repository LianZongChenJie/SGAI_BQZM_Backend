package org.jeecg.modules.bems.energyAnalysis.util;

import org.jeecg.modules.bems.energyAnalysis.vo.TableHeader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TableUtil {

    private static final DateTimeFormatter filedForMatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter minuteForMatter = DateTimeFormatter.ofPattern("HH:mm");

    public static List<TableHeader> minuteHeaders(LocalDateTime hour){
        LocalDateTime time = hour.withMinute(0).withSecond(0).withNano(0);
        // 获取startTime到endTime间的每15分钟值
        List<TableHeader> result = baseHeaders();
        result.addAll(new ArrayList<TableHeader>(){{
            add(new TableHeader(time.format(minuteForMatter),time.format(filedForMatter),false,150));
            add(new TableHeader(time.plusMinutes(15).format(minuteForMatter),time.plusMinutes(15).format(filedForMatter),false,150));
            add(new TableHeader(time.plusMinutes(30).format(minuteForMatter),time.plusMinutes(30).format(filedForMatter),false,150));
            add(new TableHeader(time.plusMinutes(45).format(minuteForMatter),time.plusMinutes(45).format(filedForMatter),false,150));
            add(new TableHeader(time.plusMinutes(60).format(minuteForMatter),time.plusMinutes(60).format(filedForMatter),false,150));
        }});
        return result;
    }

    public static List<TableHeader> dayHeaders(LocalDate localDate) {
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("H时");
        List<TableHeader> result = baseHeaders();
        int endHour = 23;
        if(LocalDate.now().equals(localDate)){
            endHour = LocalTime.now().getHour();
            if(endHour > 0){
                endHour--;
            }
        }
        result.addAll(
                IntStream.rangeClosed(0, endHour)
                        .mapToObj(hour -> {
                            TableHeader tableHeader = new TableHeader();
                            LocalDateTime localDateTime = LocalDateTime.of(localDate, LocalTime.MIN.withHour(hour));
                            tableHeader.setField(localDateTime.format(filedForMatter));
                            tableHeader.setLabel(localDateTime.format(labelFormatter));
                            return tableHeader;
                        })
                        .collect(Collectors.toList()));
        return result;
    }

    public static List<TableHeader> monthHeaders(int year, int month) {
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("d日");
        List<TableHeader> result = baseHeaders();
        int endDay = LocalDate.now().getYear() == year && LocalDate.now().getMonthValue() == month ? LocalDate.now().getDayOfMonth() : LocalDate.of(year, month, 1).lengthOfMonth();
        result.addAll(IntStream.rangeClosed(1, endDay)
                .mapToObj(day -> {
                    TableHeader tableHeader = new TableHeader();
                    LocalDate localDate = LocalDate.of(year, month, day);
                    tableHeader.setField(LocalDateTime.of(localDate, LocalTime.MIN).format(filedForMatter));
                    tableHeader.setLabel(localDate.format(labelFormatter));
                    return tableHeader;

                })
                .collect(Collectors.toList()));
        return result;
    }

    public static List<TableHeader> yearHeaders(int year) {
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("M月");
        List<TableHeader> result = baseHeaders();
        int endMonth = LocalDate.now().getYear() == year ? LocalDate.now().getMonthValue() : 12;

        result.addAll(IntStream.rangeClosed(1, endMonth).mapToObj(i -> {
            TableHeader tableHeader = new TableHeader();
            LocalDate localDate = LocalDate.of(year, i, 1);
            tableHeader.setField(LocalDateTime.of(localDate, LocalTime.MIN).format(filedForMatter));
            tableHeader.setLabel(localDate.format(labelFormatter));
            return tableHeader;
        }).collect(Collectors.toList()));
        return result;
    }

    private static List<TableHeader> baseHeaders() {
        return new ArrayList<>(Arrays.asList(new TableHeader("名称", "name", true, 180), new TableHeader("合计", "sum", true, 150)));
    }

}
