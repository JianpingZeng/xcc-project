.PHONY: all clean adpcm epic jpeg mesa pegwit g721 gsm mpeg2

JOBS=-j8
DIRNAME=test
MK_FILE= -f Makefile

all:
	$(MAKE) adpcm
	$(MAKE) bitcount
	$(MAKE) epic
	$(MAKE) jpeg
	$(MAKE) mesa
	$(MAKE) pegwit
	$(MAKE) g721
	$(MAKE) gsm
	$(MAKE) mpeg2
	$(MAKE) sha
	$(MAKE) susan

adpcm:
	$(MAKE) -C adpcm/src $(JOBS) $(MK_FILE)
bitcount:
	$(MAKE) -C bitcount/src $(JOBS) $(MK_FILE)
epic:
	$(MAKE) -C epic/src $(JOBS) $(MK_FILE)
jpeg:
	$(MAKE) -C jpeg/jpeg-6a $(JOBS) $(MK_FILE)
mesa:
	$(MAKE) -C mesa linux $(JOBS) $(MK_FILE)
pegwit:
	$(MAKE) -C pegwit/src $(JOBS) $(MK_FILE)
g721:
	$(MAKE) -C g721/src $(JOBS) $(MK_FILE)
gsm:
	$(MAKE) -C gsm $(JOBS) $(MK_FILE)
mpeg2:
	$(MAKE) -C mpeg2 $(JOBS) $(MK_FILE)
sha:
	$(MAKE) -C sha $(JOBS) $(MK_FILE)
susan:
	$(MAKE) -C susan $(JOBS) $(MK_FILE)

copy:
	-cp adpcm/src/rawdaudio $(COPYDIR)
	-cp adpcm/src/rawcaudio $(COPYDIR)
	-cp epic/src/epic $(COPYDIR)
	-cp epic/src/unepic $(COPYDIR)
	-cp jpeg/jpeg-6a/cjpeg $(COPYDIR)
	-cp jpeg/jpeg-6a/djpeg $(COPYDIR)
	-cp mesa/demos/mipmap $(COPYDIR)
	-cp mesa/demos/osdemo $(COPYDIR)
	-cp mesa/demos/texgen $(COPYDIR)
	-cp pegwit/src/pegwit $(COPYDIR)
	-cp g721/src/decode $(COPYDIR)
	-cp g721/src/encode $(COPYDIR)
	-cp gsm/bin/toast $(COPYDIR)
	-cp gsm/bin/untoast $(COPYDIR)
	-cp mpeg2/bin/mpeg2decode $(COPYDIR)
	-cp mpeg2/bin/mpeg2encode $(COPYDIR)
	-cp sha/sha $(COPYDIR)
	-cp susan/susan $(COPYDIR)

clean:
	$(MAKE) -C adpcm/src clean
	$(MAKE) -C bitcount/src clean
	$(MAKE) -C epic/src clean
	$(MAKE) -C jpeg/jpeg-6a clean
	$(MAKE) -C mesa realclean
	$(MAKE) -C pegwit/src clean
	$(MAKE) -C g721/src clean
	$(MAKE) -C gsm clean
	$(MAKE) -C mpeg2 clean
	$(MAKE) -C sha clean
	$(MAKE) -C susan clean
	$(MAKE) -C qsort/src clean
