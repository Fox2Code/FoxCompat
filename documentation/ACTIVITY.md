<-- Go back | [Index](README.md) | [Go next-->](EMBEDDED.md)

## Activity utilities

Note: For embedded restrictions, check [embedded activity page](EMBEDDED.md)

### Utils

Call `FoxActivity.setThemeRecreate(@StyleRes int resId)` 
to set a theme and recreate activity if necessary.

Call `FoxActivity.pressBack()` instead of `FoxActivity.onBackPressed()` 
when you want to simulate a back press, alternatively you can call 
`FoxActivity.forceBackPressed()` to call the original Android activity.

### Fullscreen

FoxCompat support [RikkaX inset library](https://github.com/RikkaApps/RikkaX/tree/master/insets) 
to support fullscreen layouts via xml resource files.

To get useful inset that code you can use the following methods:
`FoxActivity.getStatusBarHeight()` + `FoxActivity.getActionBarHeight()` = top inset
`FoxActivity.getNavigationBarHeight()` = bottom inset

Note: In landscape mode, `FoxActivity.getNavigationBarHeight()` usually return `0`.

### Status bar

FoxCompat add `FoxStatusBarManager` it's possible to gen an instance by calling either
`FoxActivity.getStatusBarManager()` or `FoxStatusBarManager.from(Context)`

- `FoxStatusBarManager.expandNotificationsPanel()`
- `FoxStatusBarManager.expandSettingsPanel()`
- `FoxStatusBarManager.collapsePanels()`

### Action bar

FoxCompat add utilities to control ActionBar more easily.

- `FoxActivity.hideActionBar()`
- `FoxActivity.showActionBar()`
- `FoxActivity.setActionBarBackground(Drawable)`
- `FoxActivity.setDisplayHomeAsUpEnabled(boolean)`
- `FoxActivity.setActionBarExtraMenuButton(int, MenuItem.OnMenuItemClickListener, ...)`
- `FoxActivity.removeActionBarExtraMenuButton()`

