package github.com.ioridazo.fundanalyzer.client.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 構造化ログを自動付与するメソッドアノテーション
 *
 * <p>本注釈を付けたメソッドが正常終了したとき、{@link LoggedAspect} が
 * {@link FundanalyzerLogClient#toInteractorLogObject} を構築し処理時間付きの
 * INFO ログを出力する。{@link Category} / {@link Process} / メッセージは注釈の
 * 引数で指定する。引数に {@code Document} を含むメソッドの場合、その documentId と
 * edinetCode が自動的にログコンテキストへ反映される。</p>
 *
 * <p>例外発生時はそのまま再送出される（catch しない）。例外時の構造化ログは
 * 各メソッドの try-catch で従来どおり明示的に書く。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Logged {

    /**
     * 処理カテゴリ
     *
     * @return Category
     */
    Category category();

    /**
     * 処理内容
     *
     * @return Process
     */
    Process process();

    /**
     * 完了ログに出力するメッセージ
     *
     * @return メッセージ文字列
     */
    String message();
}
