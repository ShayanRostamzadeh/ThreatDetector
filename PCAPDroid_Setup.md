To get the PCAPDroid up and running with Threat Detector follow the upcoming steps.

Step 1: Install PCAPdroid:
    Download PCAPdroid from GitHub Releases (https://github.com/emanuele-f/PCAPdroid/releases)
    Install it on your Android device.

Step 2: Enable Packet Capture:
    Open PCAPdroid.
    Tap Start Capture.
    Grant VPN permissions when prompted (required for traffic monitoring).

Step 3: Enable the TCP Server:
    To allow ThreatDetector to receive traffic logs enable the TCP exporter on the STATUS page.
    Configure the following in the settings menu:
        Collector IP Address: 127.0.0.1 (localhost)
        Collector Port: 1234 (or another port if the user wants)
        Format: PCAPNG
    Your ThreatDetector app will now be able to connect to PCAPdroid via this TCP server.

Step 4: Enable PCAPNG Extensions:
    ThreatDetector requires PCAPNG Extensions (e.g., UID-to-app mapping) to associate IP traffic with installed apps.
    Go to Settings → Capture Section → Enable PCAPDroid Extensions.
    This ensures that metadata such as app UID, package names, and process info is included.

Step 5: Connect Threat Detector
    Launch ThreatDetector app.
    The app will connect to 127.0.0.1:1234 and parse incoming packets.

It will then query AbuseIPDB for malicious IP detection.
