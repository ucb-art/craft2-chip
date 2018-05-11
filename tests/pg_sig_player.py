#!/usr/bin/python

import numpy as np
import scipy.signal as signal
import matplotlib.pyplot as plt
import math




#################################
#   inputs
#################################

# datatype, assumed to be signed fixed point
bitwidth = 8
frac_bits = 7
lanes = 32

# TODO: handle these correctly if they're not integers...
words_per_word = 64/bitwidth 

# input properties, assumed to be noisy sine wave 
snr = 10#-10 # dB
#sig_freq = 1930 # MHz
sig_freq = 417.1875 # MHz
samp_freq = 6000.0 # MHz
width = 8192

# PG properties
dur = 8192 # words in the PG
#dur = 128 # words in the PG
fft_size = 1024
repeat = 10000

#################################
#   sig gen
#################################

def gen_sine(f0, fs, dur, doppler):
  t = np.arange(dur)
  #sinusoid = np.sin(2.0*np.pi*t*(float(f0)/float(fs)))
  #sinusoid = signal.chirp(t, 0.0+float(doppler)/fs, width*2, float(f0+doppler)/fs)

  # bins are samp_freq / dec / channels wide = 6000/8/128 = 5.86 MHz
  bin_width = samp_freq/8.0/128.0
  # span some number of bins, say 3
  bin_span = 10
  sinusoid = signal.chirp(t, float(f0)/(fs), width, float(f0+bin_span*bin_width)/(fs))
  return sinusoid

def gen_noise(dur):
  noise = np.random.normal(0,1,dur)*math.pow(2, -0.5)
  return noise

def rotate(l, n):
  return l[n:] + l[:n]

def gen_sig(f0, fs, dur, snr, rot, doppler):
  mul = math.pow(10, (snr/20.0))
  sine = gen_sine(f0, fs, dur, doppler)
  #for i in range(dur):
  #  if i%repeat >width:
  #    sine[i] = 0.0
  noise = gen_noise(dur)
  sig = ((sine * mul) + noise) / (mul + 1)
  #sig = sine
  return np.array(rotate(list(sig), rot))

# assumes signal is between -1 and 1
def gen_data(signal, bitwidth, frac_bits):
  max_value = math.pow(2.0, bitwidth-1)-1  
  signal = np.round(signal * max_value / 2.0) # [stevo]: scale by extra factor of 2 to avoid clipping
  div = math.pow(2.0, frac_bits)  
  signal = signal / div
  return signal

def value_to_bits(value, bitwidth, frac_bits):
  div = math.pow(2.0, frac_bits)
  value = np.round(value * div)
  max_value = math.pow(2.0, bitwidth-1)-1
  if (value > max_value or value < -max_value):
    print "Warning: signal value outside bit range, clipping"
  if (value > max_value):
    value = max_value
  elif (value < -max_value):
    value = -max_value
  if value < 0:
    value = value + (max_value+1)*2
  return int(value)
  

#################################
#   plot/test 
#################################

freq_max = samp_freq
freqs = [x*1.0/fft_size*freq_max for x in range(fft_size)]
#x = gen_data(gen_sig(sig_freq, samp_freq, dur, snr, 0, 0), bitwidth, frac_bits)[:fft_size]
x = gen_data(gen_sig(sig_freq, samp_freq, dur, snr, 1000, 0), bitwidth, frac_bits)[:fft_size]
#x_ref = gen_data(gen_sig(sig_freq, samp_freq, dur, 100, 0, 0), bitwidth, frac_bits)[:fft_size]
#print list(x)
#fig = plt.figure()
#ax = fig.add_subplot(111)
#ax.plot(x, 'b')
y = np.fft.fft(x)
z = 20.0*np.log10(np.abs(y))
#c = np.conj(np.fft.fft(x_ref))
#d = 20.0*np.log10(np.abs(y*c))
#e = np.fft.ifft(y*c)
#fig = plt.figure()
#ax = fig.add_subplot(111)
#ax.plot(e, 'r')
fig = plt.figure()
ax = fig.add_subplot(111)
ax.plot(freqs, z, '-', linewidth=2)
#ax.plot(freqs, d, 'r', linewidth=2)
#ax.set_xlim(xmin=-10,xmax=freq_max+10)
#ax.set_ylim(ymin=-20,ymax=50)
plt.title("Tuner Output", fontsize=20)
plt.xticks(fontsize=24)
plt.yticks(fontsize=24)
#plt.gfc().subplots_adjust(bottom=0.15)
plt.xlabel("Frequency (MHz)", fontsize=24)
plt.ylabel("Power (dB)", fontsize=24)
#plt.savefig('tuner_freq.png',bbox_inches='tight')
plt.show()

#################################
#   write 
#################################


x = gen_data(gen_sig(sig_freq, samp_freq, dur, snr, 0, 0), bitwidth, frac_bits)



calib = False
with open("pg_sig_player.template.h", "r") as i:
  with open("pg_sig_player.h", "w") as o:
    for line in i:
      if 'generated data goes here' in line:
        o.write("  // generated data goes here:\n")
        o.write("  int samples = %d;\n"%(dur/words_per_word*2))
        o.write("  unsigned long test_arr[%d] = {"%(dur/words_per_word*2))
        running_value = 0
        for (count,value) in enumerate(x):
          running_value = running_value + (value_to_bits(value, bitwidth, frac_bits) << (bitwidth*(count%words_per_word)))
          if (count+1)%words_per_word == 0:
            o.write("%s\n    0x%s"%("" if count == words_per_word-1 else ",", format(running_value, 'x')))
            running_value = 0
          if (count+1)%32 == 0: # TODO: handle for other PEs correctly
            o.write(",\n    0x0000000000000000") 
            o.write(",\n    0x0000000000000000") 
            o.write(",\n    0x0000000000000000") 
            o.write(",\n    0x0000000000000000") 
        if running_value != 0:
          o.write("%s\n    0x%s"%("" if count < words_per_word-1 else ",", format(running_value, 'x')))
        o.write("};\n")
      else:
        o.write(line,)

