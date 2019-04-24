# Zeus

[![](https://jitpack.io/v/iamdennisme/Zeus.svg)](https://jitpack.io/#iamdennisme/Zeus)

Zeus make application start quickly

## Download

- Add it in your root build.gradle at the end of repositories:

```
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

- Add the dependency

```
dependencies {
     implementation 'com.github.iamdennisme:Zeus:x.y.z'
}
```

## Usage

### Create Task

```kotlin
class TestTask : Task() {

    override fun needWait(): Boolean {
        return true
    }

    override fun run() {
        Thread.sleep(2000)
        //do your init 
    }

}
```

### Init in Application

```Kotlin
class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
            Zeus.init(this)
            Zeus.createInstance().run {
                addTask(TestTask())
                    .start()
                await()
            }
    }
}
```


