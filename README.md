# aram-cardlet

This Java Card applet can work as ARA-M containing the access rules required by Android Secure Element CTS.
It is not guaranteed that this applet works for the future versions of the CTS test cases.

# Dependency

Java Card 2.2.1 was used while this applet was developed due to the limitation of the used secure element.

SIM Tools provided by the OSMOCOM (Open Source Mobile Communications) community was used for developing the applet.
You can get it from their own git repository (http://git.osmocom.org/sim/sim-tools/).
Or, it is also okay to use a forked version (https://github.com/cheeriotb/osmocom-sim-tools) which was modified just for the adaptation to Python 3 (3.7.0).

# Setup and Usage

Please see Android CTS Test for Secure Element (https://source.android.com/compatibility/cts/secure-element).
Note that make file in this git repository is just a sample one and has a deep dependency on the forked version of the OSMOCOM SIM Tools (https://github.com/cheeriotb/osmocom-sim-tools).
You should prepare your own appropriate make file for your development environment.

# Licence

This software is released under the GNU General Public License v2.0, see LICENSE.

# Author

cheeriotb (cheerio.the.bear@gmail.com)

# References

* Requirement
    * Android CTS Test for Secure Element - https://source.android.com/compatibility/cts/secure-element
* Secure Element (nano SIM)
    * sysmousim-SJS1-4FF - http://shop.sysmocom.de/products/sysmousim-sjs1-4ff
* Test Specification
    * CTS Test Code - https://android.googlesource.com/platform/cts/+/master/tests/tests/secure_element/omapi/
* Standard
    * ETSI TS 102.221 V15.0.0
    * Global Platform Card Specification Version 2.2
    * Global Platform Card Specification Version 2.1.1
    * Global Platform Secure Element Access Control Version 1.1
