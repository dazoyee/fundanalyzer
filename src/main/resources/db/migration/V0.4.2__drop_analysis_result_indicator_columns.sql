-- BPS/EPS/ROE/ROA は財務諸表値から都度計算する方式へ切り替え済み（読み取り: フェーズ1a／書き込み: フェーズ1b）で、
-- analysis_result の永続列としては使われなくなった。列自体を撤去する。
-- H2（dev/test）は複数列を1文でまとめて DROP する ALTER TABLE 構文に対応していないため、1列ずつ4文に分ける。
ALTER TABLE `analysis_result`
    DROP COLUMN `bps`;
ALTER TABLE `analysis_result`
    DROP COLUMN `eps`;
ALTER TABLE `analysis_result`
    DROP COLUMN `roe`;
ALTER TABLE `analysis_result`
    DROP COLUMN `roa`;
