; Instalátor Rainy Launcheru (NSIS)
; Instaluje se jen pro aktuálního uživatele, takže nepotřebuje admina.

Unicode true
!include "MUI2.nsh"

Name "Rainy Launcher ${VERSION}"
OutFile "${OUT}"
InstallDir "$LOCALAPPDATA\Programs\Rainy Launcher"
RequestExecutionLevel user
SetCompressor /SOLID lzma
BrandingText "Rainy Client"
VIProductVersion "${VERSION}.0"
VIAddVersionKey /LANG=1029 "ProductName" "Rainy Launcher"
VIAddVersionKey /LANG=1029 "FileDescription" "Instalátor Rainy Launcheru"
VIAddVersionKey /LANG=1029 "CompanyName" "NEBIXY"
VIAddVersionKey /LANG=1029 "FileVersion" "${VERSION}"
VIAddVersionKey /LANG=1029 "LegalCopyright" "NEBIXY"

!define MUI_ICON "${ICON}"
!define MUI_UNICON "${ICON}"
!define MUI_ABORTWARNING
!define MUI_FINISHPAGE_RUN "$INSTDIR\RainyLauncher.exe"
!define MUI_FINISHPAGE_RUN_TEXT "Spustit Rainy Launcher"

!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "Czech"

!define UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\RainyLauncher"

Section "Rainy Launcher"
  ; Když launcher běží (aktualizace), zavřít ho
  nsExec::Exec 'taskkill /IM RainyLauncher.exe /F'
  Sleep 300

  SetOutPath "$INSTDIR"
  File /r "${SRC}/*.*"

  CreateShortcut "$DESKTOP\Rainy Launcher.lnk" "$INSTDIR\RainyLauncher.exe"
  CreateShortcut "$SMPROGRAMS\Rainy Launcher.lnk" "$INSTDIR\RainyLauncher.exe"

  WriteUninstaller "$INSTDIR\Odinstalovat.exe"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayName" "Rainy Launcher"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayVersion" "${VERSION}"
  WriteRegStr HKCU "${UNINST_KEY}" "Publisher" "NEBIXY"
  WriteRegStr HKCU "${UNINST_KEY}" "DisplayIcon" "$INSTDIR\RainyLauncher.exe"
  WriteRegStr HKCU "${UNINST_KEY}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "${UNINST_KEY}" "UninstallString" '"$INSTDIR\Odinstalovat.exe"'
  WriteRegDWORD HKCU "${UNINST_KEY}" "NoModify" 1
  WriteRegDWORD HKCU "${UNINST_KEY}" "NoRepair" 1
SectionEnd

Section "Uninstall"
  nsExec::Exec 'taskkill /IM RainyLauncher.exe /F'
  Sleep 300
  Delete "$DESKTOP\Rainy Launcher.lnk"
  Delete "$SMPROGRAMS\Rainy Launcher.lnk"
  RMDir /r "$INSTDIR"
  DeleteRegKey HKCU "${UNINST_KEY}"
  ; Hra, světy a nastavení (%APPDATA%\.rainyclient) zůstanou
SectionEnd
