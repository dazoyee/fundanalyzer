@echo off

:input
set INPUT=
set /P INPUT="サービス削除処理を開始しますか？(n/Y):
if not defined INPUT (
  goto input
)
if not %INPUT% == Y (
  echo 処理を中断します。
  exit 0
)

:: サービス登録
rem sc.exe delete fundanalyzer
nssm remove fundanalyzer
