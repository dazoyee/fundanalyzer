package github.com.ioridazo.fundanalyzer.web.view.model.edinet.detail;

import github.com.ioridazo.fundanalyzer.web.view.model.edinet.EdinetListViewModel;
import lombok.Value;

import java.util.List;

@SuppressWarnings("RedundantModifiersValueLombok")
@Value(staticConstructor = "of")
public class EdinetDetailViewModel {

    // 対象提出日の処理状況
    private final EdinetListViewModel edinetList;

    // 提出日に関連する未処理ドキュメントのリスト
    private final List<DocumentViewModel> documentDetailList;
}
