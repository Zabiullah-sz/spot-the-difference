const { app, BrowserWindow } = require('electron');

let appWindow;
let detachedWindow = null;

function createDetachedWindow() {
    detachedWindow = new BrowserWindow({ width: 800, height: 600 });
    detachedWindow.loadURL(`file://${__dirname}/dist/client/index.html#/home`);
    detachedWindow.on('close', () => {
        detachedWindow = null;
    });
}
const { ipcMain } = require('electron');

ipcMain.on('detach-window', (event) => {
    if (!detachedWindow) {
        createDetachedWindow();
    }
});

ipcMain.on('attach-window', (event) => {
    if (detachedWindow) {
        detachedWindow.close();
        detachedWindow = null;
        // Signal to Angular that you need to display the chat in the main app again.
    }
});
function initWindow() {
    appWindow = new BrowserWindow({
        // fullscreen: true,
        height: 800,
        width: 1000,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false,
            enableRemoteModule: true,
            partition: 'persist:webviewsession',     
        },
    });

    // Electron Build Path
    const path = `file://${__dirname}/dist/client/index.html`;
    appWindow.loadURL(path);

    appWindow.setMenuBarVisibility(false);

    // Initialize the DevTools.
    // appWindow.webContents.openDevTools()

    appWindow.on('closed', function () {
        appWindow = null;
    });
}

app.on('ready', initWindow);

// Close when all windows are closed.
app.on('window-all-closed', function () {
    // On macOS specific close process
    if (process.platform !== 'darwin') {
        app.quit();
    }
});

app.on('activate', function () {
    if (appWindow === null) {
        initWindow();
    }
});
