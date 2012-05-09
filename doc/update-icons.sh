#!/bin/bash

cp icon.png ../res/drawable-ldpi/icon.png
mogrify -geometry 36x36  ../res/drawable-ldpi/icon.png

cp icon.png ../res/drawable-mdpi/icon.png
mogrify -geometry 48x48  ../res/drawable-mdpi/icon.png

cp icon.png ../res/drawable-hdpi/icon.png
mogrify -geometry 72x72  ../res/drawable-hdpi/icon.png


