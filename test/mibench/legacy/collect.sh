#!/bin/bash

declare -A binaries

if [ $1 == "all" ]; then
  benchs=("adpcm" "epic" "g721" "gsm" "jpeg" "mesa" "mpeg2" "pegwit" "pgp")
else
  benchs=("$1")
fi
#apps=("perlbench" "bzip2" "gcc" "mcf" "milc" "namd" "gobmk" "dealII" "soplex" "hmmer" "sjeng" "libquantum" "h264ref" "lbm" "omnetpp" "astar" "Xalan")
binaries=(["adpcm"]="rawdaudio rawcaudio" ["epic"]="epic unepic" ["g721"]="decode encode" ["gsm"]="toast  untoast" ["jpeg"]="cjpeg djpeg" ["mesa"]="mipmap osdemo texgen" ["mpeg2"]="mpeg2decode  mpeg2encode" ["pgp"]="pgp" ["pegwit"]="pegwit")
benchNum=${#benchs[@]}
HOMEDIR="/home/lqingrui"
MEDIA_DIR_BASE="${HOMEDIR}/Benchmark/mediabench3"
BUILDDIR="run/build_base_amd64-m32-gcc42-nn.0000"

UPLOADPRE='spawn scp \-P2200 '
UPLOADPOST=' lqingrui@systemg.cs.vt.edu:mediabenchrun\/'
SPECHOME='\/home\/lqingrui\/Benchmark\/cpu2006\/benchspec\/CPU2006'
BENCHBUILDDIR='run\/build_base_amd64-m32-gcc42-nn.0000'

for i in ${benchs[*]}; do
  pushd ${MEDIA_DIR_BASE}/${i}
  # firstly compile the source code
  if [ ${i} == "mesa" ]; then
    make realclean;
    make linux -j8
  elif [ ${i} == "jpeg" ]; then
    pushd jpeg-6a && make clean && make -j8
  elif [ ${i} == "gsm" ]; then
    make clean && make -j8
  elif [ ${i} == "mpeg2" ]; then
    make clean && make -j8
  elif [ ${i} == "pgp" ]; then
    pushd rsaref && pushd source
    make clean && make -j8
    popd && popd
    pushd src && make clean && make -j8
  else
    pushd src && make clean && make -j8
  fi
  if [ $? -ne 0 ]; then
    echo "${i} cannot make successfully!"
    exit 1 
  fi
  for TARGET in ${binaries[${i}][*]}; do
    if [ ${i} == "jpeg" ]; then
      echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
      du -b jpeg-6a/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
#      mv /home/lqingrui/dupInsts ~/Benchmark/mediabench3/result/${TARGET}
    elif [ ${i} == "mesa" ]; then
      echo -e "texgen \c" >> ~/Benchmark/mediabench3/codesize.txt
      du -b demos/texgen | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
#      mv /home/lqingrui/dupInsts ~/Benchmark/mediabench3/result/texgen
    elif [ ${i} == "gsm" ]; then
      echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
      du -b bin/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
#      mv /home/lqingrui/dupInsts ~/Benchmark/mediabench3/result/${TARGET}
    elif [ ${i} == "mpeg2" ]; then
      echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
      du -b bin/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
#      mv /home/lqingrui/dupInsts ~/Benchmark/mediabench3/result/${TARGET}
    else
      echo -e "${TARGET} \c" >> ~/Benchmark/mediabench3/codesize.txt
      du -b src/${TARGET} | xargs echo | cut -d' ' -f1 >> ~/Benchmark/mediabench3/codesize.txt
#      mv /home/lqingrui/dupInsts ~/Benchmark/mediabench3/result/${TARGET}
    fi
    if [ $? -ne 0 ]; then
      echo "${i} cannot get Binary size!!!"
      exit 1 
    fi
  done
  popd
done



