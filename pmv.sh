#!/bin/sh
# Rename old i18n properties across all localized properties files and
# references within java source.
#
# NOTE: these settings target the editor properties files;
# you must change srcdir/destdir to target the framework properties files.
destdir=build/classes/abbot/editor/i18n
srcdir=src/abbot/editor/i18n

if [ "$#" = "0" ]; then
    echo "usage: pmv <oldname> <newname> [<oldname1> <newname1> ...]"
    exit 1
fi

if [ "$1" = "-v" ]; then
    verbose=true
    shift
fi
if [ "$1" = "-n" ]; then
    n=echo
    shift
fi

ant -q i18n
tmp=/tmp/encoding.$$

while [ $# -ge 2 ]; do
    old=$1
    new=$2
    sedargs="$sedargs -e s/^$old=/$new=/g -e s/^$old.desc=/$new.desc=/g -e s/^$old.acc=/$new.acc=/g -e s/^$old.mn=/$new.mn=/g -e s/^$old.title=/$new.title=/g -e s/^$old.msg=/$new.msg=/g"
    sed1args="$sed1args -e s/Strings.get(\"$old\"/Strings.get(\"$new\"/g"
    shift 2
done

for file in $destdir/StringsBundle*.properties; do
    locale=$(echo $file | sed 's/.*StringsBundle_*\(.*\).properties/\1/g')

    if [ -z "$locale" ]; then
        target=$srcdir/StringsBundle.properties
    elif [ "$locale" = "en_US" ]; then
        continue
    else
        target=$(echo $srcdir/StringsBundle_$locale.*)
    fi

    if [ -n "$verbose" ]; then
        echo "Patching $file ($locale) to $target"
    fi

    tmpfile=$(cygpath -m $tmp.$locale)
    sed $sedargs $file > $tmpfile

    if [ -n "$verbose" ]; then echo "Converting back to source"; fi
    case $target in
        *.txt) $n mv $tmpfile $target ;;
        *.cp1250) $n native2ascii -encoding Cp1250 -reverse $tmpfile $target ;;
        *.cp1252) $n native2ascii -encoding Cp1252 -reverse $tmpfile $target ;;
        *.unicode) $n native2ascii -encoding unicode -reverse $tmpfile $target ;;
        *.properties) $n mv $tmpfile $target ;;
        *) echo "Unknown encoding '$target'" ;;
    esac
    $n rm -f $tmpfile
done    

src=$(find src -name '*.java')
for file in $src; do
    tmpfile=$tmp.$(basename $file)
    if [ -n "$verbose" ]; then
        echo "Patching $file to $tmpfile"
    fi
    sed $sed1args $file > $tmpfile
    $n mv $tmpfile $file
done
