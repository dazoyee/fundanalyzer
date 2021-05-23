package github.com.ioridazo.fundanalyzer.client.csv.bean;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class EdinetCsvResultBean {

    @CsvBindByName(column = "ＥＤＩＮＥＴコード", required = true)
    private String edinetCode;

    @CsvBindByName(column = "提出者種別")
    private String type;

    @CsvBindByName(column = "上場区分")
    private String listCategories;

    @CsvBindByName(column = "連結の有無")
    private String consolidated;

    @CsvBindByName(column = "資本金")
    private int capitalStock;

    @CsvBindByName(column = "決算日")
    private String settlementDate;

    @CsvBindByName(column = "提出者名", required = true)
    private String submitterName;

    @CsvBindByName(column = "提出者名（英字）")
    private String englishName;

    @CsvBindByName(column = "提出者名（ヨミ）")
    private String japaneseName;

    @CsvBindByName(column = "所在地")
    private String location;

    @CsvBindByName(column = "提出者業種")
    private String industry;

    @CsvBindByName(column = "証券コード")
    private String securitiesCode;

    @CsvBindByName(column = "提出者法人番号")
    private String corporationNumber;
}
