#define MyAppName "鑫客主播工作台"
#ifndef MyAppVersion
#define MyAppVersion "0.3.4"
#endif
#define MyAppExeName "XinkeAnchorMetricsProbe.exe"
#ifndef WebViewBootstrapper
  #error WebViewBootstrapper must point to the verified Microsoft installer
#endif
#ifndef MyPublishDir
  #error MyPublishDir must point to the verified fresh publish directory
#endif
#if !FileExists(MyPublishDir + "\ui\anchor-desktop.html")
  #error Workbench entry page missing from publish output
#endif
#if !FileExists(MyPublishDir + "\desktop-assets.json")
  #error Workbench resource manifest missing from publish output
#endif
#ifdef InstallerValidation
  #define DesktopShortcutRoot "{app}\validation-desktop"
  #define StartupShortcutRoot "{app}\validation-startup"
#else
  #define DesktopShortcutRoot "{autodesktop}"
  #define StartupShortcutRoot "{userstartup}"
#endif

[Setup]
#ifdef InstallerValidation
AppId=XinkeAnchorInstallerValidation
Uninstallable=no
#else
AppId={{69F9F09D-841F-4416-B58A-4652A6F136D7}
#endif
AppName={#MyAppName}
AppVersion={#MyAppVersion}
ArchitecturesAllowed=x64compatible
MinVersion=10.0
DefaultDirName={localappdata}\XinkeAnchorMetricsProbe
PrivilegesRequired=lowest
CloseApplications=yes
OutputDir=C:\Users\adzuki\Desktop
#ifdef InstallerValidation
OutputBaseFilename=xinke-anchor-installer-validation-{#MyAppVersion}
#else
OutputBaseFilename=xinke-anchor-metrics-agent-setup-{#MyAppVersion}
#endif
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
UninstallDisplayName={#MyAppName}
UninstallDisplayIcon={app}\{#MyAppExeName}
SetupIconFile=app.ico
UsePreviousTasks=yes
InfoAfterFile=help\安装后须知.txt

[Tasks]
Name: "desktopicon"; Description: "创建桌面快捷方式"; Flags: checkedonce
Name: "startup"; Description: "登录 Windows 后自动启动（后台托盘，不自动上报）"; Flags: checkedonce

[Files]
Source: "{#MyPublishDir}\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "{#WebViewBootstrapper}"; Flags: dontcopy

[InstallDelete]
Type: files; Name: "{#StartupShortcutRoot}\{#MyAppName}.lnk"; Tasks: not startup
Type: files; Name: "{#DesktopShortcutRoot}\{#MyAppName}.lnk"; Tasks: not desktopicon

[Icons]
#ifdef InstallerValidation
Name: "{app}\validation-startmenu\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{app}\validation-startmenu\鑫客主播工作台使用说明"; Filename: "{app}\help\使用说明.html"
#else
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autoprograms}\鑫客主播工作台使用说明"; Filename: "{app}\help\使用说明.html"
#endif
Name: "{#DesktopShortcutRoot}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon
Name: "{#StartupShortcutRoot}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Parameters: "--startup"; Tasks: startup

[UninstallDelete]
Type: files; Name: "{#StartupShortcutRoot}\{#MyAppName}.lnk"

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "启动鑫客主播工作台"; Flags: nowait postinstall skipifsilent

[Code]
var
  PreviousTasksRestored: Boolean;
  RuntimeInstallAttempted: Boolean;
  RuntimeInstallSucceeded: Boolean;

function HasRuntimeAtKey(RootKey: Integer): Boolean;
var
  Version: String;
  DotPosition, Major: Integer;
begin
  Version := '';
  RegQueryStringValue(RootKey, 'Software\Microsoft\EdgeUpdate\Clients\{F3017226-FE2A-4295-8BDF-00C3A9A7E4C5}', 'pv', Version);
  DotPosition := Pos('.', Version);
  if DotPosition > 0 then Version := Copy(Version, 1, DotPosition - 1);
  Major := StrToIntDef(Version, 0);
  Result := Major >= 109;
end;

function HasWebViewRuntime: Boolean;
begin
#ifdef InstallerValidation
  if ExpandConstant('{param:RUNTIMETEST|}') <> '' then begin
    Result := (ExpandConstant('{param:RUNTIMETEST|}') = 'present') or RuntimeInstallSucceeded;
    Exit;
  end;
#endif
  Result := HasRuntimeAtKey(HKCU) or HasRuntimeAtKey(HKLM32);
  if not Result and IsWin64 then Result := HasRuntimeAtKey(HKLM64);
end;

function EnsureWebViewRuntime(var NeedsRestart: Boolean): String;
var
  RuntimeExitCode: Integer;
  Executed: Boolean;
begin
  Result := '';
  if HasWebViewRuntime then begin
    Log('WebView2 runtime detected; prerequisite installation skipped.');
    Exit;
  end;
  Result := 'Microsoft Edge WebView2 Runtime 尚未安装成功。请运行分享包中的 MicrosoftEdgeWebView2RuntimeInstallerX64.exe，完成后重新运行本安装包。';
  if RuntimeInstallAttempted then Exit;
  RuntimeInstallAttempted := True;
  Log('WebView2 runtime missing; installing Microsoft prerequisite.');
  WizardForm.StatusLabel.Caption := '正在安装 Microsoft Edge WebView2 Runtime，请稍候…';
#ifdef InstallerValidation
  if ExpandConstant('{param:RUNTIMETEST|}') <> '' then begin
    RuntimeInstallSucceeded := ExpandConstant('{param:RUNTIMETEST|}') = 'missing-success';
    if RuntimeInstallSucceeded then Result := '';
    Exit;
  end;
#endif
  ExtractTemporaryFile('MicrosoftEdgeWebview2Setup.exe');
  Executed := Exec(ExpandConstant('{tmp}\MicrosoftEdgeWebview2Setup.exe'), '/silent /install', '', SW_HIDE, ewWaitUntilTerminated, RuntimeExitCode);
  Log(Format('WebView2 installer executed=%d exit=%d', [Ord(Executed), RuntimeExitCode]));
  if Executed and HasWebViewRuntime then begin
    Result := '';
    if RuntimeExitCode = 3010 then NeedsRestart := True;
  end;
end;

procedure RestorePreviousTasks;
begin
  if PreviousTasksRestored then Exit;
  PreviousTasksRestored := True;
  { The app directory is available only after wizard initialization. }
  if (ExpandConstant('{param:TASKS|}') = '') and FileExists(ExpandConstant('{app}\{#MyAppExeName}')) then begin
    if FileExists(ExpandConstant('{#StartupShortcutRoot}\{#MyAppName}.lnk')) then
      WizardSelectTasks('startup')
    else
      WizardSelectTasks('!startup');
    if FileExists(ExpandConstant('{#DesktopShortcutRoot}\{#MyAppName}.lnk')) then
      WizardSelectTasks('desktopicon')
    else if GetPreviousData('DesktopTasksConfigured', '') = 'yes' then
      WizardSelectTasks('!desktopicon');
  end;
end;

procedure CurPageChanged(CurPageID: Integer);
begin
  if CurPageID = wpSelectTasks then RestorePreviousTasks;
end;

function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  { Silent installs do not visit the task-selection page. }
  RestorePreviousTasks;
  Result := EnsureWebViewRuntime(NeedsRestart);
end;

procedure RegisterPreviousData(PreviousDataKey: Integer);
begin
  SetPreviousData(PreviousDataKey, 'DesktopTasksConfigured', 'yes');
end;

