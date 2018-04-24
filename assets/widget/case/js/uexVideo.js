/**
 * Created by ylt on 16/8/24.
 */

if (UNIT_TEST) {
    var uexVideoCase = {
        "openPlayer": function () {
            openPlayer();
            UNIT_TEST.assert(true);
        }
    };

    function openPlayer() {
        var param = {
            src: "res://video.mp4",
            startTime: 3,
            autoStart: true,
            forceFullScreen: false,
            showCloseButton: true,
            showScaleButton: true,
            isAutoEndFullScreen: true,
            canSeek:false,
            width: 650,
            height: 480,
            x: 100,
            y: 100,
            scrollWithWeb: true
        };
        uexVideo.openPlayer(JSON.stringify(param));
    }

    UNIT_TEST.addCase("uexVideo", uexVideoCase);
}