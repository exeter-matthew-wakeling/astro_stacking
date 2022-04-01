# astro_stacking
Software to manipulate photographs of space.

This software is a motley collection of random tools that I have written that can process photographs of space. Jobs that can be performed are:
1. Analysis of photos, to calculate the base brightness, the star brightness, and the blurriness of the stars. This is used to identify photos that are cloud-contaminated, out of focus, taken too far into sunset/sunrise, affected by dew on the optics, or with wobble or tracking issues. These photos should be rejected, and only good photos should be stacked.
2. Alignment and stacking of multiple photos together, to improve the signal to noise ratio.
3. Combine multiple stacked photos together as a weighted average.
4. Tone-map the high-dynamic-range photo to an output image.
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
3. Something that can read PNM image files, so that you can view the result. GIMP (available from https://www.gimp.org/) will happily read PNM files. Alternatively you can install pnmtojpeg, which is part of the netpbm software collection, available from http://netpbm.sourceforge.net/getting_netpbm.php or your package manager if using Linux.

## Usage
