[<-- Go back](ACTIVITY.md) | [Index](README.md) | Go next-->

## Embedded

### Embedded activity

Embedded activities are activities embeddable with some code restrictions.

While often `MyActivity extends FoxActivity implements FoxActivity.Embeddable` is enough to 
make an activity embeddable keep in mind the following:
- The activity state might not be kept across recreate.
- The activity can't control action bar when embedded.
- `createLayoutInflaterFactory()` will not be called.
- `getApplication()` will return null when embedded.
- Application callbacks will not execute.
  (Except FoxCompat specific callbacks)
- `finish()` will not be executable.

### Make an external Embedded activity

Making an externally embeddable activity put more restrictions, 
into making the Activity on top of a normal embedded activity, 
but allow cross class-loader activity embedding.

This mean `FoxActivity.loadApplicationIntoProcess(app)` and 
`FoxActivityView.createFromLoadedComponent(component)` can be used to 
load an `ExternallyEmbeddable` activity, even if not from the same application.

note: It is recommended to not call `loadApplicationIntoProcess` on main application process, 
as loading multiples applications on the same process can create instability.

Using `MyActivity extends FoxActivity implements FoxActivity.ExternallyEmbeddable` implies 
`MyActivity extends FoxActivity implements FoxActivity.Embeddable`, 
this mean an externally embeddable activity can still be used as a normal embeddable activity.

The additional restrictions are the following:
- `getSupportFragmentManager()` can't be called under any circumstances.
- `getViewModelStore()` instance might not stay across config changes.
