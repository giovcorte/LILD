# LILD
LightImageLoaderDownloader

Simple but really efficient image loader and downloader, with a memory and disk cache.

ImageLoader.get().load("https://....png").intoView(imageView).run() // To load an url into an ImageView object

ImageLoader.get().load(file).intoView(imageView).run() 

ImageLoader.get().load(R.drawable.image).intoView(imageView).run()

ImageLoader.get().cache(cacheStrategy).load("https://....png").intoView(imageView).run() // To specify the caching strategy from IImageCache.ImageStrategy

ImageLoader.get().tag(customCacheTag).load("https://....png").intoView(imageView).run()

ImageLoader.get().load("https://....png").intoView(imageView, progressDrawable, errorDrawable).run() // progress and error drawables are not mandatory

ImageLoader.get().load("https://....png").intoCache().run() // To warm up the cache

ImageLoader.get().load("https://....png").intoFile(file).run() // To download the image to the given file

ImageLoader.get().load(request, fetcher).into(target).run() // To load your custom implementatios 

ImageLoader.get().cache() // To get the IImageCache instance

ImageLoader.get().cache().dumps(request, file) // To save to the given file the given cached request. If you want to download a cached image knowing only the key, you can obtain a valid request object through Request.just(key)
