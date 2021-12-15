
VERSION = 1.2-4
VERSION_ = 1.2.4
JAVAC = javac
JAVA = java
JAR = jar
JAVADOC = javadoc

# If you're building with java8, you can uncomment this to reduce warnings
JAVADOC_FLAGS = -Xdoclint:all,-missing

JARFILE = jcdf.jar

WWW_FILES = $(JARFILE) javadocs index.html cdflist.html cdfdump.html
WWW_DIR = /homeb/mbt/public_html/jcdf

TEST_JARFILE = jcdf_test.jar
TEST_CDFS = data/example1.cdf data/example2.cdf data/test.cdf data/local/*.cdf
TEST_BADLEAP = data/test_badleap.cdf
NASACDFJAR = nasa/cdfjava_3.6.0.4.jar
NASALEAPSECFILE = nasa/CDFLeapSeconds.txt

ARTIFACT_PKG = jcdf-$(VERSION_)
ARTIFACTS = $(ARTIFACT_PKG).jar \
            $(ARTIFACT_PKG)-sources.jar \
            $(ARTIFACT_PKG)-javadoc.jar

# See https://central.sonatype.org/publish/publish-manual/
# Uses gpg and ~/.m2/settings.xml
SIGN_AND_DEPLOY = \
    mvn gpg:sign-and-deploy-file \
       -Durl=https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/ \
       -DrepositoryId=ossrh -DpomFile=pom.xml

JSRC = \
       BankBuf.java \
       Buf.java \
       Bufs.java \
       Pointer.java \
       SimpleNioBuf.java \
       WrapperBuf.java \
       \
       AttributeDescriptorRecord.java \
       AttributeEntryDescriptorRecord.java \
       CdfDescriptorRecord.java \
       CompressedCdfRecord.java \
       CompressedParametersRecord.java \
       CompressedVariableValuesRecord.java \
       GlobalDescriptorRecord.java \
       Record.java \
       RecordFactory.java \
       RecordPlan.java \
       SparsenessParametersRecord.java \
       UnusedInternalRecord.java \
       VariableDescriptorRecord.java \
       VariableIndexRecord.java \
       VariableValuesRecord.java \
       CdfField.java \
       OffsetField.java \
       \
       BitExpandInputStream.java \
       Compression.java \
       DataReader.java \
       NumericEncoding.java \
       RunLengthInputStream.java \
       RecordMap.java \
       \
       AttributeEntry.java \
       CdfContent.java \
       GlobalAttribute.java \
       VariableAttribute.java \
       Variable.java \
       CdfInfo.java \
       CdfReader.java \
       DataType.java \
       Shaper.java \
       CdfFormatException.java \
       EpochFormatter.java \
       TtScaler.java \
       \
       CdfDump.java \
       CdfList.java \
       LogUtil.java \

TEST_JSRC = \
       ExampleTest.java \
       SameTest.java \
       OtherTest.java \
       BufTest.java \

build: jar docs artifacts

jar: $(JARFILE)

docs: $(WWW_FILES)

artifacts: artifacts.zip

javadocs: $(JSRC) package-info.java
	rm -rf javadocs
	mkdir javadocs
	$(JAVADOC) $(JAVADOC_FLAGS) -quiet \
                   -d javadocs $(JSRC) package-info.java

index.html: jcdf.xhtml
	xmllint -noout jcdf.xhtml && \
	xmllint -html jcdf.xhtml >index.html

cdflist.html: $(JARFILE)
	./examples.sh \
            "java -classpath $(JARFILE) uk.ac.bristol.star.cdf.util.CdfList" \
            "-help" \
            "data/example1.cdf" \
            "-data data/example1.cdf" \
            >$@

cdfdump.html: $(JARFILE)
	./examples.sh \
            "java -classpath $(JARFILE) uk.ac.bristol.star.cdf.util.CdfDump" \
            "-help" \
            "data/example1.cdf" \
            "-fields -html data/example1.cdf" \
            >$@

installwww: $(WWW_DIR) $(WWW_FILES)
	rm -rf $(WWW_DIR)/* && \
	cp -r $(WWW_FILES) $(WWW_DIR)/

updatewww: $(WWW_DIR)/index.html

$(WWW_DIR)/index.html: index.html
	cp index.html $@

$(NASALEAPSECFILE):
	curl 'https://cdf.gsfc.nasa.gov/html/CDFLeapSeconds.txt' >$@

test: build buftest extest othertest badleaptest convtest

convtest: $(JARFILE) $(TEST_JARFILE)
	rm -rf tmp; \
	mkdir tmp; \
	for f in $(TEST_CDFS); \
        do \
           files=`./cdfvar.sh -outdir tmp -report $$f`; \
           cmd="java -ea -classpath $(JARFILE):$(TEST_JARFILE) \
                     uk.ac.bristol.star.cdf.test.SameTest $$files"; \
           ./cdfvar.sh -outdir tmp -create $$f && \
           echo $$cmd && \
           $$cmd || \
           break; \
        done

extest: $(JARFILE) $(TEST_JARFILE)
	jargs="-ea \
               -classpath $(JARFILE):$(TEST_JARFILE) \
               uk.ac.bristol.star.cdf.test.ExampleTest \
               data/example1.cdf data/example2.cdf data/test.cdf" && \
	java -Duser.timezone=GMT $$jargs && \
	java -Duser.timezone=PST $$jargs && \
	java -Duser.timezone=EET $$jargs && \
        java $$jargs

othertest: $(JARFILE) $(TEST_JARFILE) $(NASACDFJAR) $(NASALEAPSECFILE)
	jargs="-ea \
               -classpath $(JARFILE):$(TEST_JARFILE):$(NASACDFJAR) \
               uk.ac.bristol.star.cdf.test.OtherTest" && \
	export CDF_LEAPSECONDSTABLE=$(NASALEAPSECFILE) && \
	java -Duser.timezone=GMT $$jargs && \
	java -Duser.timezone=PST $$jargs && \
	java -Duser.timezone=EET $$jargs && \
	java $$jargs

buftest: $(JARFILE) $(TEST_JARFILE)
	java -ea \
             -classpath $(JARFILE):$(TEST_JARFILE) \
             uk.ac.bristol.star.cdf.test.BufTest

badleaptest: $(JARFILE) $(TEST_BADLEAP)
	# This one should run OK
	java -classpath $(JARFILE) uk.ac.bristol.star.cdf.util.CdfDump \
             $(TEST_BADLEAP) >/dev/null
	# but this one should report that the file's leap seconds table
	# is out of date and exit with a RuntimeException
	if java -classpath $(JARFILE) \
                uk.ac.bristol.star.cdf.util.CdfList -data \
                $(TEST_BADLEAP) >/dev/null 2>&1; then \
            should_have_failed; \
        fi

uploadmaven: $(ARTIFACTS)
	$(SIGN_AND_DEPLOY) -Dfile=$(ARTIFACT_PKG).jar
	$(SIGN_AND_DEPLOY) -Dfile=$(ARTIFACT_PKG)-sources.jar \
                           -Dclassifier=sources
	$(SIGN_AND_DEPLOY) -Dfile=$(ARTIFACT_PKG)-javadoc.jar \
                           -Dclassifier=javadoc
	@echo
	@echo "Now close and release from staging repository https://s01.oss.sonatype.org/"
	@echo "see https://central.sonatype.org/publish/release/"

clean:
	rm -rf $(JARFILE) $(TEST_JARFILE) tmp \
               index.html javadocs cdflist.html cdfdump.html \
               $(ARTIFACTS) artifacts.zip \
               $(ARTIFACT_PKG).jar.asc \
               $(ARTIFACT_PKG)-sources.jar.asc \
               $(ARTIFACT_PKG)-javadoc.jar.asc \
               pom.xml.asc

$(JARFILE): $(JSRC)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -source 1.5 -target 1.5 -d tmp $(JSRC) \
            && echo "$(VERSION)" >tmp/uk/ac/bristol/star/cdf/jcdf.version \
            && $(JAR) cf $@ -C tmp .
	rm -rf tmp

$(TEST_JARFILE): $(JARFILE) $(TEST_JSRC)
	rm -rf tmp
	mkdir -p tmp
	$(JAVAC) -Xlint:unchecked -d tmp -classpath $(JARFILE) $(TEST_JSRC) \
            && $(JAR) cf $@ -C tmp .
	rm -rf tmp

artifacts.zip: $(ARTIFACTS)
	jar cfM $@ $(ARTIFACTS)

$(ARTIFACT_PKG).jar: $(JARFILE)
	cp $(JARFILE) $@

$(ARTIFACT_PKG)-sources.jar: $(JSRC)
	$(JAR) cf $@ $(JSRC)

$(ARTIFACT_PKG)-javadoc.jar: javadocs
	$(JAR) cf $@ -C javadocs .

