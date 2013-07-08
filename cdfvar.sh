#!/bin/sh

# Command-line utility to generate various versions of a CDF file.
# An input file is specified on the command line, and this script
# rewrites different versions of it with varying endianness,
# compression style, CDF format version etc.  These changes affect
# the format, but not the content, of the file.
# The resulting files can then be compared with the original to see
# if the library finds the same content in them all, which is a good
# test of the handling of different endiannesses, compression styles,
# CDF format versions etc.
#
# Flags:
#   -create       - actually writes the files
#   -report       - just output the filenames that would be written on stdout
#   -verbose      - be verbose
#   -outdir <dir> - directory for output files
#   -help         - usage
#
# Dependencies:
#   The cdfconvert command (from the CDF distribution) must be on the path.

usage="Usage: $0 [-create] [-report] [-verbose] [-outdir <dir>] <cdf-file>"
report=0
create=0
verbose=0
outdir=""
cdf=""
while [ $# -gt 0 ]
do
   case "$1" in
      -r|-report)
          report=1
          ;;
      -c|-create)
          create=1
          ;;
      -outdir)
          outdir=$2
          shift
          ;;
      -v|-verbose)
          verbose=1
          ;;
      -h|-help)
          echo $usage
          exit 0
          ;;
      *.cdf)
          cdf="$1"
          ;;
      *)
          echo $usage
          exit 1
   esac
   shift
done

if [ -z "$cdf" ]
then
   echo $usage
   exit 1
fi

# echo "create: $create; report: $report; verbose: $verbose; cdf: $cdf"
basein=`echo $cdf | sed 's/\.cdf$//'`
if [ -z "$outdir" ]
then
   outdir=`dirname $basein`
fi
if [ -n "$outdir" ]
then
   outdir="$outdir"/
fi
baseout=${outdir}`basename $basein`

# Some CDFs contain data types which cannot be converted to CDF V2.6 format.
# Attempting a conversion with the "-backward" backward compatibility flag
# on these will cause a partial file conversion which is not easy to detect,
# resulting in CDFs with different content.  Add a filename-based hack
# to ensure that no attempt is made to force backward-compatibility
# checks for some files that are known to cause trouble.
if echo $cdf | egrep -q 'rbsp-|tha_|test.cdf'
then
   back_compat=""
else
   back_compat=-backward
fi

# Add more sets of cdfconvert flags here to do different manipulations
# of the CDF file.
count=0
for cflags in \
  "-row -encoding network -compression cdf:gzip.5" \
  "-column -encoding ibmpc -compression vars:huff.0" \
  "$back_compat -compression vars:rle.0" \
  "-sparseness vars:srecords.no -compression cdf:ahuff.0 -checksum md5"
do
   count=`echo $count+1 | bc`
   outname="${baseout}_c${count}"
   cmd="cdfconvert -delete $cflags $basein $outname"
   test $report -gt 0 && echo ${outname}.cdf
   if [ $create -gt 0 ]
   then
      if [ $verbose -gt 0 ]
      then
         echo $cmd
         $cmd
      else
         $cmd >/dev/null
      fi
   else
      true
   fi
done


