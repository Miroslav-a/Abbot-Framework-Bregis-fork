#
# Makefile to build abbot, tests and examples
# NOTE: If you're on w32 and don't already have GNU make, get
# cygwin from www.cygwin.com, which will allow you to run this
# Makefile's targets from a real shell.
#


#
# Edit JAVA_HOME and ANT_HOME to suit your environment
# Everything else should be location-agnostic
#
       OS = $(shell uname -s | sed -e 's/CYGWIN.*/w32/g')
ifeq ($(OS),w32)
        P =;
#      DEV = //comte/development
      DEV = c:
else
        P =:
      DEV = /nfs/dev
ifeq ($(OS),Darwin)
   VMARGS = -Xdock:name=$@
endif
endif
# ANT_HOME = $(DEV)/ant
      ANT = export CLASSPATH="lib/junit-4.8.2.jar$Plib/jakarta-oro-2.0.7.jar"; ant

       VM = java $(VMARGS)
    RUNCP = lib/abbot.jar$Plib/costello.jar$Plib/example.jar
     LIBS = build/jgraph.jar$Pbuild/junit-4.8.2.jar$Pbuild/gnu-regexp-1.1.4.jar$Pbuild/jdom-1.1.1.jar$Pbuild/groovy-all-1.8.1.jar
 TCLASSES = build/test-classes
   TESTCP = $P$(TCLASSES)$Pbuild/classes$P$(LIBS)
     CODE = example.MyCode
    SUITE = example.MyCodeTest

all: 
	$(ANT) 

doc clean dist version: 
	$(ANT) $@

# Run the script editor
# "classes" must be included in the classpath in order to be able to browse
# for test suites/test cases (the test case collector is kinda broken).
edit: run-editor
run-editor: Costello
Costello:
	cp build/*.jar lib
	$(VM) $(VMARGS) -cp "$(RUNCP)" abbot.editor.Costello "$(SUITE)" $(ARGS)

install: 
	$(ANT) tgz
	tar xzf abbot-$(VERSION).tgz -C $(DEV)/abbot-versions
	(cd $(DEV); rm -f abbot; ln -s abbot-versions/abbot-$(VERSION) abbot)

#
# Use Ant where it makes sense.
#
test: unit-tests
unit-tests: all
	cp build/*.jar lib
	@if [ -n "$(CASE)" ]; then \
	  echo $(VM) $(VMARGS) -cp "$(TESTCP)" $(CASE) $(TEST) $(ARGS); \
	  $(VM) $(VMARGS) -cp "$(TESTCP)" $(CASE) $(TEST) $(ARGS); \
	else \
	  echo $(VM) $(VMARGS) -cp "$(TESTCP)" abbot.UnitTestSuite $(ARGS); \
	  $(VM) $(VMARGS) -cp "$(TESTCP)" abbot.UnitTestSuite $(ARGS); \
   	fi

# Upload to sourceforge
 HTMLDIR = /home/groups/a/ab/abbot/htdocs
 VERSION = $(shell sed -n 's/.*"abbot\.version" value="\(.*\)".>.*/\1/p' build.xml)
     URL = frs.sourceforge.net:uploads/
     FTP = rsync -avP -e ssh 
sf-release: 
	-$(FTP) abbot-$(VERSION).tgz abbot-$(VERSION).zip $(URL)
	scp abbot.tgz shell.sf.net:$(HTMLDIR)
	ssh shell.sf.net "(cd $(HTMLDIR);tar xzf abbot.tgz)"
	@echo "Don't forget to register the file release"

# Upload html to sourceforge
publish-html:
	scp README.shtml abbot.sf.net:$(HTMLDIR)
	scp doc/*.shtml abbot.sf.net:$(HTMLDIR)/doc

publish-news:
	scp doc/news.shtml abbot.sf.net:$(HTMLDIR)/doc

publish-reports:
	tar czf reports.tgz -C doc/reports .
	scp reports.tgz abbot.sf.net:$(HTMLDIR)
	archive="archived/$$(date +%y%m%d)"; \
	ssh abbot.sf.net "(cd $(HTMLDIR);\
          tar xzf reports.tgz -C doc/reports;\
          mkdir -p doc/reports/$$archive;\
          tar xzf reports.tgz -C doc/reports/$$archive);\
          rm -f reports.tgz"
#EOF
