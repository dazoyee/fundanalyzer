package github.com.ioridazo.fundanalyzer.domain.value;

/**
 * 企業の上場状況
 */
public enum ListingStatus {

    /** 上場中 */
    LISTED,

    /** 上場廃止 */
    DELISTED,

    /** 取得先との通信に失敗するなどして確認できなかった */
    UNKNOWN
}
