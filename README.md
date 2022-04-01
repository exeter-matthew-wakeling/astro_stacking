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
dcraw -w -W -t 0 -C <red> <blue> <file>
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
Replacing all the relevant bits in that command. The software will name all its output files starting with the output prefix. Use the \<red\> and \<blue\> numbers that you determined a couple of sections earlier. This may take some time. The software memory-maps all of the input PGM files, and maps them onto the output in a single step, so there are no intermediate mapped images. The software will produce two output files - the first is `\<output_prefix\>_sourceCount.pgm` which is a greyscale image where the brightness is how many source images were used. The second is `\<output_prefix\>_image.pfm` which is a 3-channel 32-bit floating point image in a similar format to PNM.

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

## Stacking multiple stacked images
The images to be stacked together can be taken using different cameras, with different settings, at different resolutions, different angles and focal lengths. Some of this functionality has not yet been written. Firstly, the images need to be converted to a common format.

### Converting images.
I have provided several programs to convert images from TIFF and FITS format into 32-bit float PNM-like format, and from PNM-like format into TIFF format. These are:
1. java Convert16bitToFloatPnm \<input_file\>.pnm \>\<output_file\>.pfm - converts from an 8-bit or 16-bit PNM into 32-bit float PNM-like format. To convert from 16-bit TIFF format, first use tifftopnm, which is part of the netpbm software collection.
2. java Convert32bitTifToPnm \<input_file\>.tiff \>\<output_file\>.pfm - converts from a 32-bit float TIFF file into a 32-bit float PNM-like format.
3. java ConvertFitsToFloatPnm \<input_file\>.fits \>\<output_file\>.pfm - converts from a 32-bit float FITS file into a 32-bit float PNM-like format.
4. java ConvertToLinearPpm \<input_file\>.pfm \>\<output_file\>.pnm - converts from a 32-bit float PNM-like format to 16-bit linear PNM format. This can be further converted into a 16-bit TIFF file using pnmtotiff, which is part of the netpbm software collection.
5. java Convert32bitPnmToTif \<input_file\>.pfm \>\<output_file\>.tiff - converts from a 32-bit float PNM-like format to a 32-bit float TIFF format.

Note that if an input TIFF/FITS image is a greyscale single-channel image, then a 3-channel 32-bit float PNM-like file will still be created, but with all 3 channels containing identical values. This is likely to change in the future - I plan to extend the file format to allow a number of channels that isn't 3. For processing, all the input files must be in the 32-bit float PNM-like format. They can be converted to TIFF or tone-mapped once the final result is made.

### Aligning the images
This part is not yet written, because I didn't need it for the M82 BAT image. I'll need it for the Andromeda image though, and it won't take long to write.

### Masking the input images
Sometimes parts of the input images have pixel values that are incorrect and must be ignored. For example, in the M82 BAT data, the joelkuiper image doesn't cover the whole target image, and the edges have been antialiased. That antialiased edge presents itself as a very dark pixels, which is false and can introduce artefacts, so it must be masked off. To mask off an image, tone-map it into an image that can be loaded into an image editor such as GIMP, then add a second layer, and draw over the areas that must be masked off, so that the areas that are OK are black, and the areas that must be ignored are white. Then save the image as a PNM file, and run the following:
```
java MaskPfm <input_file>.pfm <mask>.pnm ><output_file>.pfm
```

### Measuring the brightness and noise level of each image
In order to combine multiple images, it is necessary to measure how bright the features are, and how noisy the background is. To do this, we need to pick an area that has no stars, galaxies, or nebulae. The brightness and noise in this area will be measured. We also need to pick an area that has a reasonably consistent brightness that comes from galaxy or nebulae emissions, without too many distinguishable stars. The brightness in this area will be measured. The areas are defined by drawing a box around them, and specifying the x-y coordinates of the top left corner and the bottom right corner. This step should be performed when the multiple images are aligned against each other, and after any gradients have been removed. The areas analysed should be the same for all the images to be combined. The command is:
```
java BrightnessAndNoise <input_file>.pfm <dark_left_x> <dark_top_y> <dark_right_x> <dark_bottom_y> <light_left_x> <light_top_y> <light_right_x> <light_bottom_y>
```
The software will output a single row with multiple tab-separated values, which are:
1. The name of the input file
2. The mean red brightness of the dark region
3. The red noise level in the dark region
4. The mean red brightness of the bright region
5. The red weight for combining the multiple images
6. The mean green brightness of the dark region
7. The green noise level in the dark region
8. The mean green brightness of the bright region
9. The green weight for combining the multiple images
10. The mean blue brightness of the dark region
11. The blue noise level in the dark region
12. The mean blue brightness in the bright region
13. The blue weight for combining the multiple images

### Combining multiple images
To combine multiple images, we use the weights from the previous step, and run:
```
java WeightedSum <file1>.pfm <weight1> <file2>.pfm <weight2> ... ><output>.pfm
```
I will be changing this process shortly, because it doesn't allow for different weights for the three channels, and it isn't completely sensible about what to do when one image has no values over an area.

Once the final image has been created, it can be tone mapped as described above.

## Removing gradients from images
Often, light pollution background of a space image has a gradient, so it is not simply a matter of subtracting a constant value to cancel the light pollution out. The following software will add the specified gradient, hopefully to cancel it out. You can discover what the values should be by running BrightnessAndNoise as described earlier, and specifying areas to the left/right/top/bottom.
```
java AddGradient <input_file>.pfm <red_base> <red_x> <red_y> <green_base> <green_x> <green_y> <blue_base> <blue_x> <blue_y> ><output_file>.pfm
```

## How to combine subs with different cameras/settings/temperature
To combine sub-frames taken using different settings, we should first align all the sub-frames to the same target image. The different cameras/settings/temperatures will require different dark/flat/flatdark frames, and so StitchBayer needs to be run separately for the different groups, and then the results can be combined.
1. Sort the images into categories as above, placing light frames into separate groups by their camera/settings.
2. Convert the images into PGM files as above.
3. Determine the chromatic aberration for each group, and convert to PNM and JPEG as above.
4. Run ProduceMatches2 and run hugin to align *all* of the sub-frames together.
5. Split the project.pto file into sections. In hugin, delete images so that only images of a single group are present. Then, don't change the layout or perform any optimisation, but save the project under a different name for that group. Reload the complete project, and repeat for the other groups.
6. For each group, run StitchBayer on the relevant project_pgm.pto file, with the correct darks/flats/flatdarks.
7. Run WeightedSum to combine the multiple output files into one. Choose the weights either by measuring the noise using BrightnessAndNoise, or by logic from the actual parameters.
8. Run ToneMap as above.

If the separate groups are taken from the same camera but with different settings, then it may be possible to work out what the weights should be. The weight for each group should be the brightness divided by the square of the dark noise. So, say we have 20 light frames taken with a 2 minute exposure, and 30 frames taken with a 1 minute exposure. The 2-minute exposure images should have twice the brightness of the 1-minute exposures, but the square of the dark noise (assuming the noise is dominated by light pollution shot noise) is also twice as large per exposure. The square of dark noise is inversely proportional to the number of sub-frames (same assumption). So, the 20 frames of 2 minutes should be given a weight of 20, and the 30 frames of 1 minute should be given a weight of 30. The 20 frames of 2 minutes will contribute more to the output image, even though the weight is lower, because they are brighter.

## Todo list
This software is not finished. Here are some things that are left to do:
1. Improve file format, to allow a number of colour channels other than three. Better still allow the channels to be named.
2. Write the software to remap stacked images.
3. Improve the stacking of multiple images - combine the measurement of noise and brightness with the weighted summing software, so that we don't have to copy across a whole load of numbers from one program to another.
4. Improve the stacking of multiple images - implement a multi-resolution spline to cope with source images with significantly different resolution.
5. Switch the integration algorithm in StitchBayer from mean-reject-constant-percentage to sigma-kappa.
6. Implement a pixel-maths program for generalised combining of different colour channels, such as merging Ha with Red. I did this in a bespoke manner for the M82 BAT, coding it directly in Java, but it would be much nicer to be able to specify maths on the command line.
7. Allow for Bayer arrangements other than RGGB, and also mono cameras.
8. Improve the gradient removal, to automatically optimise a flat gradient and a radial gradient.
