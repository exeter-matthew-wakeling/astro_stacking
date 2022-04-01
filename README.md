# astro_stacking
Software to manipulate photographs of space.

This software is a motley collection of random tools that I have written that can process photographs of space. Jobs that can be performed are:
1. Analysis of photos, to calculate the base brightness, the star brightness, and the blurriness of the stars. This is used to identify photos that are cloud-contaminated, out of focus, taken too far into sunset/sunrise, affected by dew on the optics, or with wobble or tracking issues. These photos should be rejected, and only good photos should be stacked.
2. Alignment and stacking of multiple photos together, to improve the signal to noise ratio.
3. Combine multiple stacked photos together as a weighted average.
4. Remove background gradients from images.
5. Tone-map the high-dynamic-range photo to an output image.

And a few other tasks as necessary. The software is under regular development, because I am using it, and I keep needing to do new things.

## Running Java
This software is written purely in Java. It depends on two external libraries, which are:
1. A TIFF library, available from https://github.com/ngageoint/tiff-java
2. A FITS library, available from https://github.com/nom-tam-fits/nom-tam-fits

You should download the JAR file for both of these libraries, then download all the `*.java` files from this repository.

Running this software requires that the library JAR files and the java from this repository are in the Java classpath, in order for Java to find them. This can be done in two ways. The first option is to set the CLASSPATH environment variable:
```
export CLASSPATH=/path/to/tiff.jar:/path/to/fits.jar:/path/to/astro_stacking_directory
```
The ":" character separates the three parts of this path, to specify that code can be found in these places. The second option is to specify the "-cp" option every time you run java, like this:
```
java -cp /path/to/tiff.jar:/path/to/fits.jar:/path/to/astro_stacking_directory blah blah blah
```
For all subsequent code fragments, where "java" or "javac" is specified, it is assumed that the classpath is correctly configured as specified above, either by adding the "-cp" option or using the CLASSPATH environment variable.

## Compiling
Compiling the code is then done by:
```
javac *.java
```
in the directory you downloaded this respository into (with the classpath set up correctly as above).

## Other necessary software
You will also need to have installed the following software:
1. dcraw, available from https://www.dechifro.org/dcraw/ or your package manager if using Linux - this converts images from a vast range of camera raw formats into PNM format, which my software processes.
2. hugin, available from http://hugin.sourceforge.net/ or your package manager if using Linux - this is a general image alignment and panorama generating software package, well regarded for use in daytime photography. It is used to optimise the alignment of the source photos.
3. Something that can read PNM image files, so that you can view the result. GIMP (available from https://www.gimp.org/) will happily read PNM files, as can most image viewers on Linux. Alternatively you can install pnmtojpeg, which is part of the netpbm software collection, available from http://netpbm.sourceforge.net/getting_netpbm.php or your package manager if using Linux.
4. GNU Parallel, available from https://www.gnu.org/software/parallel/ or your package manager if using Linux, isn't strictly necessary, but it can make processing go by quicker.
5. Some way to plot data points from a text file on a graph, for instance gnuplot, available from http://www.gnuplot.info/ or your package manager if Linux. Yes, you could use Excel for that if you like pain.

There are several workflows that can be used with this software, depending on the task to be performed. The two main tasks are stacking multiple sub-frames taken with the same camera together, and stacking together multiple stacked images that may be taken with different cameras and settings.

## Stacking multiple sub-frames
All photos should be taken in raw format. For these instructions, I will assume that all the photos are named something.NEF, because I have a Nikon DSLR, but it should also work for other cameras with different raw file formats. Just change something.NEF to whatever file names your camera produces.

### Image categories
Firstly, sort the images into separate categories. Put the files into a directory for each category. The categories are:
1. Light frames. These are the in-focus images of the target, all taken with the same camera settings, and hopefully the same approximate temperature. You want to have as many of these as you can.
2. Dark frames. These are images with the lens cap on the camera, all taken with the same camera settings and temperature as the light frames. You want to have a reasonable number of these. At least ten is good, 20 is better.
3. Flat frames. These are images taken of a flat white field, with the same ISO setting, F-number, and focus as the light frames, but with a different exposure time. This can be achieved in multiple ways - one way is to point the camera at a computer monitor or tablet with a completely white screen, or you can put a couple of layers of white T-shirt over the aperture and point it at an even part of the dusk sky when it's light enough to have no stars visible. The exposure time should be adjusted so that the in-camera histogram shows a peak approximately in the middle, and all the flat frames should have the same exposure time.
4. Flat-dark frames. These are images taken with the lens cap on the camera, all taken with the same camera settings as the flat frames.
5. Rejected frames. These are photos that would be light frames, except something went wrong, like for instance clouds got in the way, or the focus wasn't right, or the tripod wobbled or didn't track properly.
6. Other frames. These are any other frames, for instance if you were taking pictures in order to work out where in the sky the camera was pointing, to get the target framed correctly.

In the first four of these categories, we need to convert the camera raw files into PGM files, which contain the raw pixel values. If you're using Linux, Mac, or Windows Subsystem for Linux, or any sane command-line shell, and you have GNU Parallel installed, then the following command will do the job. Run it in each of the four directories:
```
ls -1 *.NEF | parallel dcraw -E -4 -t 0 {}
```
The rest of the work will all take place in the light frames directory.

### Image selection
  
Next, we need to analyse all the light frames, and work out if any of them should be rejected. Run the following in the light frames directory:
```
java BayerHistogram -mean >means
```
This produces statistics on each image in the directory. The output file contains one row per image, with several columns separated by tabs. The columns are:
1. The file name.
2. The mean brightness of the image, after rejecting the darkest 1% and brightest 1% of the pixels.
3. The mean brightness of the brightest 1% of the pixels.
4. The mean brightness of the brightest 0.03% of the pixels.
5. The mean brightness of the brightest 0.001% of the pixels. This may be clipped - if it is, then use column 4 instead.
6. The number of stars that are reasonably bright and aren't clipped.
7. The FWHM blurriness of those stars.
8. The full width of 1/20 maximum brightness of those stars.

You should plot these on a graph, which will allow you to spot any images that are clear outliers. For instance, to plot the mean brightness of the image using gnuplot, run gnuplot, and enter the following command:
```
plot 'means' using 0:2
```
If there are clouds contaminating your images, then typically the mean brightness (column 2) will go up (unless you're in a Bortle 1 region) as more light is reflected off the clouds, and the highlights (column 4 or 5) will reduce in brightness. The FWHM (column 7) may also increase. Focus, wobble, and tracking problems will cause the FWHM to increase too. Good images are those with a low number in column 2, a high number in columns 3-5, and a low number in columns 7 and 8. Images that do not pass this test should be moved to the rejected frames category, and you can delete the \*.pgm files for them too.

### Producing guide images, and determining chromatic aberration correction
In the light frames directory, choose a single image, and run dcraw on it, like this:
```
dcraw -w -W -t 0 -C <red> <blue> <file
```
where \<red\> and \<blue\> are numbers very close to 1, and \<file\> is the name of the camera raw file. Dcraw will convert the raw image to a PNM image file. The red and blue channels of the image will be enlarged/shrunk by the factor that you have specified. Look at stars in the very extreme corners of the image in an image viewer. You should adjust the \<red\> and \<blue\> numbers until the stars in the corner have as little chromatic aberration as possible. For instance, for my main lens, I use "-C 1.0005 1.0001". Note these two numbers. Once you have established this, we need to convert *all* the raw files, and additionally convert them to jpegs, which can be done by:
```
ls -1 *.NEF | parallel dcraw -w -W -t 0 -C <red> <blue> {}
ls -1 *.ppm | sed -e "s/\.ppm//" | parallel 'pnmtojpeg {}.ppm >{}.jpg'
ls -1 *.ppm | sed -e "s/\.ppm//" | parallel 'exiftool -tagsFromFile {}.NEF -overwrite_original {}.jpg'
```

### Aligning the images
In the light frames directory, run the following:
```
java ProduceMatches2 *.ppm >matches
```
This will identify stars in the images, and work out which stars are the same in different images. It will then output a list of control points matching stars in different images. This then later used to align the images.

Then run hugin. On the "Photos" tab, click on the "Add images" button. Navigate to the light frames directory, and select *every* \*.jpg file in the directory. Save the project as something like "project.pto", and quit hugin.

Then, add the contents of the "matches" file to the end of the "project.pto" file. This can either be done in your favourite text editor, or by running:
```
cat matches >>project.pto
```
And load the project file into hugin again. You will now need to optimise the layout of the images in hugin. This generally involves the following steps:
1. Click the "Re-optimise" button on the top toolbar strip. This may take a while if you have lots of images.
2. Open the fast preview panorama (from the top toolbar strip), then select the "Projection" tab, and click "Fit", then change the projection to rectilinear. You may want to change the field of view numbers manually, and go to the Move/drag tab to frame your image correctly. Then close the preview window.
3. Go to the Stitcher tab, and click on "Calculate optimal size". This determines the resolution of the output image. You can change it - for instance if you increase it then the image will effectively be drizzled while stacking.
4. Click the "Re-optimise" button again. This will now tell you the correct control point error. Hopefully it will be around 1 pixel or less, but it is probable that some control points will be just plain wrong, and some will be badly adjusted.
5. Open the control point list (Show control points, on the top toolbar strip), and click on the "Distance" header, to sort by decreasing distance. Shift-select all the control points with a distance greater than around 50, and press delete.
6. Click "Re-optimise" again. Repeat step 5, but this time delete control points with a distance greater than 10. There might not be any.
7. Go to the "Edit" menu, and click on "Fine-tune all points". This may take a few minutes. If you have lots of images, then you may also need to ensure you have lots of RAM/swap available, due to a bug in hugin with when it chooses to get rid of images from RAM.
8. Click "Re-optimise" again, and open the control point list. Most of the control points will have a lower distance, but the fine-tuning may have switched the point to the wrong nearby star, so some control points may have a distance that is very high again. Delete any with a distance above 5.
9. If your images are all stacked on top of each other very accurately, for instance if you used guiding and didn't dither, then that is all we can do. If the images shifted around a bit (for instance, you weren't guiding, or you did some quite big dithering), then it might be advantageous to correct for barrel distortion of the optics. Generally, you need the images to be shifted by at least 15% of their size before this will give sane results. If you have done a mosaic, then it will be very useful indeed. Go to the "Photos" tab, and under "Optimise"->"Geometric", select "Custom parameters". A new "Optimiser" tab should appear - go to that tab. On the bottom half of the window, control-click the zeros under the headings "a", "b", and "c", which should turn them bold. This allows the optimiser to calculate values for these lens parameters, which describe the barrel distortion. Then click "Re-optimise" again, and check that it produces good results, as in the control point distances aren't too high (some might need deleting), and the image doesn't appear too distorted in the preview. If the preview looks distorted, then change the values back to zero, unselect them, and re-optimise. If you have really good separation in your images, like if you did a mosaic, then you could also enable optimisation of parameters d and e.
10. Open the preview, and make sure it is still well framed.
11. Save the project, and quit hugin.

### Stacking the images
Copy the project.pto file to project_pgm.pto. Edit the new file, and search/replace ".jpg" with ".pgm". Next, run the actual stacking process, by running:
```
java StitchBayer project_pgm.pto <output_prefix> -black <dark_directory>/*.pgm -flat <flat_directory>/*.pgm -flatdark <flatdark_directory>/*.pgm -threads <number of CPU cores> -C <red> <blue>
```
Replacing all the relevant bits in that command. The software will name all its output files starting with the output prefix. Use the \<red\> and \<blue\> numbers that you determined a couple of sections earlier. This may take some time. The software memory-maps all of the input PGM files, and maps them onto the output in a single step, so there are no intermediate mapped images. The software will produce two output files - the first is `\<output_prefix\>_sourceCount.pgm` which is a greyscale image where the brightness is how many source images were used. The second is `\<output_prefix\>_image.pfm` which is a 3-channel 32-bit floating point image.

### Tone-mapping the result
We need to determine the best amount of light pollution to subtract from the image. To do this, run:
```
Java PercentilesFloat <output_prefix>_image.pfm
```
which will print out the brightness percentiles for the three colour channels. Take the numbers from the 10th percentile, and run the tone mapping:
```
java ToneMap <output_prefix>_image.pfm -ramp <ramp> -black <10%_red> <10%_green> <10%_blue> ><output_prefix>_tone.pnm
```
The \<ramp\> number is how much you want the dark areas of the image to be brightened. The tone-mapping applies a logarithmic curve to the brightness so that full brightness pixels are unchanged, but very dark pixels are brightened by the factor you specify as the \<ramp\>. The colour is preserved as the brightness is changed. You may also want to set the following parameters:
1. -white \<number\> which sets the maximum brightness clipping level. The software normally calculates this itself as the minimum of the brightest pixel in the three colour channels. If you set this higher, then the three colour channels may clip at different levels, and the brightest stars may be rendered as purple. If you set it lower, then that would allow dimmer objects such as galaxies and nebulae to be brightened.
2. -mult \<red\> \<green\> \<blue\> which sets the colour balance. Use this if the white balance is incorrect. If you shoot your flat frames with a computer monitor or pad, then you probably won't need to use this, but if you use sky flats then you probably will.
