@echo off

:input
set INPUT=
set /P INPUT="�T�[�r�X�o�^�������J�n���܂����H(n/Y):
if not defined INPUT (
  goto input
)
if not %INPUT% == Y (
  echo �����𒆒f���܂��B
  exit 0
)

:: �T�[�r�X�o�^
rem sc.exe create fundanalyzer binPath= "C:\fundanalyzer\bin\start.bat" start= auto
nssm install fundanalyzer
