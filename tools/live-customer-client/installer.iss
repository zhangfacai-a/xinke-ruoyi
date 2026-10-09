#define MyAppName "鑫客直播采集客户端"
#define MyAppVersion "2.4.0"
#define MyAppPublisher "鑫客"
#define MyAppExeName "XinkeLiveCustomer.exe"

[Setup]
AppId={{B39072F2-2D44-4B69-8CB5-4B8C7D70C2A1}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\XinkeLiveCustomer
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\..\releases
OutputBaseFilename=xinke-live-customer-setup-{#MyAppVersion}
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayIcon={app}\{#MyAppExeName}

[Code]
function GetChromePath(Param: String): String;
begin
  if FileExists(ExpandConstant('{autopf}\Google\Chrome\Application\chrome.exe')) then
    Result := ExpandConstant('{autopf}\Google\Chrome\Application\chrome.exe')
  else if FileExists(ExpandConstant('{autopf32}\Google\Chrome\Application\chrome.exe')) then
    Result := ExpandConstant('{autopf32}\Google\Chrome\Application\chrome.exe')
  else
    Result := ExpandConstant('{localappdata}\Google\Chrome\Application\chrome.exe');
end;

[Files]
Source: "..\..\build\live-customer-client\XinkeLiveCustomer.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "..\..\chrome-extensions\douyin-live-customer-v2\*"; DestDir: "{app}\chrome-extensions\douyin-live-customer-v2"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autodesktop}\{#MyAppName}"; Filename: "{code:GetChromePath}"; WorkingDir: "{app}"; Parameters: "--no-first-run --no-default-browser-check --new-window ""https://anchor.douyin.com/anchor/dashboard?from=default"""; IconFilename: "{code:GetChromePath}"
Name: "{autodesktop}\鑫客采集插件（首次安装）"; Filename: "{code:GetChromePath}"; WorkingDir: "{app}"; Parameters: "--no-first-run --no-default-browser-check --new-window ""chrome://extensions/"""; IconFilename: "{code:GetChromePath}"
Name: "{userstartup}\{#MyAppName}"; Filename: "{code:GetChromePath}"; WorkingDir: "{app}"; Parameters: "--no-first-run --no-default-browser-check --new-window ""https://anchor.douyin.com/anchor/dashboard?from=default"""; IconFilename: "{code:GetChromePath}"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "立即打开直播采集客户端"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}\chrome-extensions"
