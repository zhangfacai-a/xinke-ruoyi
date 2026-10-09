#define MyAppName "鑫客直播页面启动器"
#define MyAppVersion "1.0.0"
#define MyAppExeName "XinkeLiveCustomerOpener.exe"

[Setup]
AppId={{A45FC62D-8B38-4E5D-9D39-1D2B1C3E8F10}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={localappdata}\XinkeLiveCustomerOpener
PrivilegesRequired=lowest
OutputDir=..\..\releases
OutputBaseFilename=xinke-live-customer-opener-setup-{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
SetupIconFile=app.ico
UninstallDisplayName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}

[Files]
Source: "..\..\build\live-customer-opener\XinkeLiveCustomerOpener.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "app.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--settings"; IconFilename: "{app}\app.ico"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "立即打开抖音主播后台"; Flags: nowait postinstall skipifsilent

[UninstallRun]
Filename: "{sys}\reg.exe"; Parameters: "delete ""HKCU\Software\Microsoft\Windows\CurrentVersion\Run"" /v XinkeLiveCustomerOpener /f"; Flags: runhidden

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
Type: files; Name: "{userstartup}\鑫客直播页面启动器.lnk"
Type: files; Name: "{userstartup}\直播采集启动.lnk"
Type: files; Name: "{userstartup}\鑫客直播采集客户端.lnk"
Type: files; Name: "{autodesktop}\鑫客直播页面启动器.lnk"
