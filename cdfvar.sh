#!/bin/sh

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


count=0
for cflags in \
  "-row -encoding network -compression cdf:gzip.5" \
  "-column -encoding ibmpc -compression vars:huff.0" \
  "-backward -compression vars:rle.0" \
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


