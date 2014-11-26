define(['jquery', 'scope', 'util'], function ($, scope, util) {

    var encodeArray = function (array) {
        var encoded = "";
        var last_index = array.length - 1;
        array.forEach(function (value, index) {
            encoded += value;
            encoded += index != last_index ? "-" : "";
        });
        return encoded;
    }
    var decodeArray = function (encoded_array) {
        var decoded_array = new Array();
        encoded_array.split("-").forEach(function (element) {
            if (element.trim() != '') {
                decoded_array.push(parseInt(element))
            }
        });
        return decoded_array;
    }

    return{
        metric: 0,
        churn: [],
        workload: [],
        peer_configuration: [],
        experiment_duration: [],
        training_duration: [],
        clustering_algorithm: [],
        feedback_enabled: [],
        encode: function () {
            var encoded = this.metric + "_";
            encoded += encodeArray(this.churn) + "_";
            encoded += encodeArray(this.workload) + "_";
            encoded += encodeArray(this.peer_configuration) + "_";
            encoded += encodeArray(this.experiment_duration) + "_";
            encoded += encodeArray(this.training_duration) + "_";
            encoded += encodeArray(this.clustering_algorithm) + "_";
            encoded += encodeArray(this.feedback_enabled) + "_";
            encoded += scope.data_type;

            return encoded;
        },
        decode: function (encoded) {

            if (encoded === undefined || encoded.trim() == '') {
                this.metric = 0;
                this.churn = [];
                this.workload = [];
                this.peer_configuration = [];
                this.experiment_duration = [];
                this.training_duration = [];
                this.clustering_algorithm = [];
                this.feedback_enabled = [];
            } else {

                encoded.split("_").forEach(function (value, index) {

                    switch (index) {
                        case 0:
                            this.metric = parseInt(value);
                            break;
                        case 1:
                            this.churn = decodeArray(value);
                            break;
                        case 2:
                            this.workload = decodeArray(value);
                            break;
                        case 3:
                            this.peer_configuration = decodeArray(value);
                            break;
                        case 4:
                            this.experiment_duration = decodeArray(value);
                            break;
                        case 5:
                            this.training_duration = decodeArray(value);
                            break;
                        case 6:
                            this.clustering_algorithm = decodeArray(value);
                            break;
                        case 7:
                            this.feedback_enabled = decodeArray(value);
                            break;
                        case 8:
                            scope.data_type = value;
                            break;
                        default:
                            console.log("unknown encoded query format", index, value)
                            break;
                    }
                }, this);
            }
            return this;
        },
        init: function () {
            this.decode(location.hash.slice(1));
            $("#data_type_chooser label").removeClass("active");
            $("#data_type_chooser input:radio[value='" + scope.data_type + "']").parent().addClass("active");
            this.update();
        },
        update: function () {

            window.location.hash = this.encode();
            $("#main_title").text(scope.metrics[this.metric].name);
            $("#chart_list .active").removeClass("active");
            $("#metric_" + this.metric).addClass("active");



            this.churn.forEach(function (selection) {
                $("#churn_" + selection).prop('checked', true);
            })
            this.workload.forEach(function (selection) {
                $("#workload_" + selection).prop('checked', true);
            })
            this.peer_configuration.forEach(function (selection) {
                $("#peer_configuration_" + selection).prop('checked', true);
            })
            this.experiment_duration.forEach(function (selection) {
                $("#experiment_duration_" + selection).prop('checked', true);
            })
            this.training_duration.forEach(function (selection) {
                $("#training_duration_" + selection).prop('checked', true);
            })
            this.clustering_algorithm.forEach(function (selection) {
                $("#clustering_algorithm_" + selection).prop('checked', true);
            })
            this.feedback_enabled.forEach(function (selection) {
                $("#feedback_enabled_" + selection).prop('checked', true);
            })

            var matches = this.matches();
            var metric = scope.metrics[this.metric]
            scope.matches = matches;
            scope.metric = metric;
            scope.renderer()

//            var matches_nav = $('#matches_nav');
//            var matches_content = $('#matches_content');
//            matches_nav.empty();
//            matches_content.empty();
//
//
//            matches.forEach(function (match, index) {
//
//                matches_nav.append('<li><a href="#' +
//                    match.name +
//                    '" data-toggle="tab"' +
//                    ' id="match_link_' +
//                    match.name +
//                    '" class="small" >' +
//                    '<span class="glyphicon glyphicon-stop" style="color: ' +
//                    theme.colours[index] +
//                    '"/> ' +
//                    match.name +
//                    "</a></li>");
//
//                var div = $('<div class="tab-pane" id="' + match.name + '"></div>');
//                var table = $('<table class="table small"></table>');
//                Object.keys(match).forEach(function (key) {
//                    table.append('<tr>' +
//                        '<td style="width: 15%"><strong>' +
//                        key +
//                        '</strong></td>' +
//                        '<td>' +
//                        match[key] +
//                        '</td>' +
//                        '</tr>')
//
//                })
//                div.append(table);
//                matches_content.append(div);
//            })
//
//            $('#matches_nav a:first').tab('show');
////            $('#filter').collapse('hide');
//            $('#matches').collapse('show');
            util.resizeElementHeight(document.getElementById("chart_list"));
        },
        matches: function () {

            var matches = new Array();
            var churn_keys = Object.keys(scope.scenarios.groupBy('churn'));
            var workload_keys = Object.keys(scope.scenarios.groupBy('workload'));
            var peer_configuration_keys = Object.keys(scope.scenarios.groupBy('peer_configuration'));
            var experiment_duration_keys = Object.keys(scope.scenarios.groupBy('experiment_duration'));
            var training_duration_keys = Object.keys(scope.scenarios.groupBy('training_duration'));
            var clustering_algorithm_keys = Object.keys(scope.scenarios.groupBy('clustering_algorithm'));
            var feedback_enabled_keys = Object.keys(scope.scenarios.groupBy('feedback_enabled'));
            this.churn.forEach(function (selection) {

                scope.scenarios.groupBy('churn')[churn_keys[selection]].forEach(function (element) {
                    matches.push(element);
                });
            }, this)

            matches = matches.filter(function (element) {
                var found = false;
                this.workload.forEach(function (selection) {
                    found = found || element.workload == workload_keys[selection];
                });
                return found;
            }, this)
            matches = matches.filter(function (element) {
                var found = false;
                this.peer_configuration.forEach(function (selection) {
                    found = found || element.peer_configuration == peer_configuration_keys[selection];
                });
                return found;
            }, this)
            matches = matches.filter(function (element) {
                var found = false;
                this.experiment_duration.forEach(function (selection) {
                    found = found || element.experiment_duration == experiment_duration_keys[selection];
                });
                return found;
            }, this)
            matches = matches.filter(function (element) {
                var found = false;
                this.training_duration.forEach(function (selection) {
                    found = found || element.training_duration == training_duration_keys[selection];
                });
                return found;
            }, this)
            matches = matches.filter(function (element) {
                var found = false;
                this.clustering_algorithm.forEach(function (selection) {
                    found = found || element.clustering_algorithm == clustering_algorithm_keys[selection];
                });
                return found;
            }, this)
            matches = matches.filter(function (element) {
                var found = false;
                this.feedback_enabled.forEach(function (selection) {
                    found = found || element.feedback_enabled == feedback_enabled_keys[selection];
                });
                return found;
            }, this)
            return matches;
        }
    }
})
;