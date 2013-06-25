#!/bin/sh

# Utility file to generate an HTML file with several example invocations
# of a command-line tools
#
# Usage:
#   examples.sh <base-cmd> <args1> <args2> ...

echo "<html>"
echo "<body>"
echo "<pre>"
basecmd="$1"
shift
while [ $# -gt 0 ]
do
   args="$1"
   shift
   echo "<hr>"
   echo "% $basecmd $args"
   echo "<div style='background-color:lavender'>"
   $basecmd $args
   echo "</div>"
done
echo "</pre>"
echo "</body>"
echo "</html>"
