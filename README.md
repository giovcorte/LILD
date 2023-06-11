# LILD
LightImageLoaderDownloader

Simple and ultra-light weight image loader for Views and Compose! Caching both on disk and memory, and ability to transfer cached images on files to save them. Supports loading of urls, files and res ids.

```kotlin

imageLoader = ImageLoader(applicationContext)
request =  ImageRequestBuilder("https://cdn.britannica.com/85/235885-050-C8CC6D8B/Samoyed-dog-standing-snow.jpg")
                .placeHolder(getDrawable(R.drawable.ic_launcher_foreground)!!)
                .build()
// inside composable function
AsyncImage(
   imageLoader,
   request,
   modifier = Modifier
       .clip(shape = CircleShape)
       .size(size = 62.dp),
   contentDescription = "lucy pic",
   contentScale = ContentScale.Crop
)

// for views
imageLoader.load(request, imageView)

```
