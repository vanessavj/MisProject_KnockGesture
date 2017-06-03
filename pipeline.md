# Pipeline

## Accelerometer Data

+ record and save in accelData[[x_values], [y_values], [z_values], [magnitude_values]]
	+ if raw values are not needed later on, dont use them

+ save magnitude values in fft_x[435]

+ split fft_x & fft_y in 14 parts

+ calc mean of fft of every part and save in exra array

==> patternarray [mean_0, mean_1, ..., mean_13]


## Recognize knocks

+ search array of processed data for patterns
	+ (start with first significant peak)

+ compare to known patterns
+ choose the one with smallest difference, if close enough
