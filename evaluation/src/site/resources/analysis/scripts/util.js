define(
    ['jquery', 'json'],
    function () {

        var alphanum = function (a, b) {
            function chunkify(t) {
                t = '' + t;
                var tz = new Array();
                var x = 0, y = -1, n = 0, i, j;

                while (i = (j = t.charAt(x++)).charCodeAt(0)) {
                    var m = (i == 46 || (i >= 48 && i <= 57));
                    if (m !== n) {
                        tz[++y] = "";
                        n = m;
                    }
                    tz[y] += j;
                }
                return tz;
            }

            var aa = chunkify(a);
            var bb = chunkify(b);

            for (x = 0; aa[x] && bb[x]; x++) {
                if (aa[x] !== bb[x]) {
                    var c = Number(aa[x]), d = Number(bb[x]);
                    if (c == aa[x] && d == bb[x]) {
                        return c - d;
                    } else return (aa[x] > bb[x]) ? 1 : -1;
                }
            }
            return aa.length - bb.length;
        }

        Array.prototype.groupBy = function (property_name, alias_function) {
            var groups = {};
            this.forEach(function (element) {
                var key = alias_function == undefined ? element[property_name] : alias_function(element[property_name]);

                if (groups[key] === undefined) {
                    groups[key] = new Array();
                }
                groups[key].push(element);
            });
            return groups;
        };
        Array.prototype.sortBy = function (property_name) {
            this.sort(function (one, other) {

                var onep = one[property_name];
                var otherp = other[property_name];

                if(onep == 'Heavy'){
                    onep= 0+ onep;
                } else if(onep =='Medium'){
                    onep = 1+ onep;
                }else if(onep == 'Light'){
                    onep = 2 + onep;
                }


                if(otherp == 'Heavy'){
                    otherp= 0+ otherp;
                } else if(otherp =='Medium'){
                    otherp = 1+ otherp;
                }else if(other == 'Light'){
                    otherp = 2 + otherp;
                }

                return alphanum(onep, otherp)
//                return alphanum(one[property_name], other[property_name]);
            })
            return this;
        };
        Array.prototype.addIfAbsent = function (element) {
            if (this.indexOf(element) == -1) {
                this.push(element);
                return true;
            }
            return false;
        }
        Array.prototype.removeIfExists = function (element) {
            var index = this.indexOf(element);
            if (index > -1) {
                this.splice(index, 1);
                return true;
            }
            return false;
        }
        String.prototype.hashCode = function () {
            var hash = 0;
            if (this.length == 0) return hash;
            for (i = 0; i < this.length; i++) {
                char = this.charCodeAt(i);
                hash = ((hash << 5) - hash) + char;
                hash = hash & hash;
            }
            return hash;
        }
        String.prototype.startsWith = function (str) {
            return this.indexOf(str) == 0;
        };
        String.prototype.contains = function (str) {
            var number = this.indexOf(str);
            console.log(str + ' ' + number)

            return number != -1;
        };
        String.prototype.toTitleCase = function () {
            return this.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
        };

        return{

            color: {
                increaseBrightness: function (hex, percent) {
                    hex = hex.replace(/^\s*#|\s*$/g, '');
                    if (hex.length == 3) {
                        hex = hex.replace(/(.)/g, '$1$1');
                    }

                    var r = parseInt(hex.substr(0, 2), 16),
                        g = parseInt(hex.substr(2, 2), 16),
                        b = parseInt(hex.substr(4, 2), 16);

                    return '#' +
                        ((0 | (1 << 8) + r + (256 - r) * percent / 100).toString(16)).substr(1) +
                        ((0 | (1 << 8) + g + (256 - g) * percent / 100).toString(16)).substr(1) +
                        ((0 | (1 << 8) + b + (256 - b) * percent / 100).toString(16)).substr(1);
                }
            },


            convert: {
                nanosecondToMillisecond: function (nanoseconds) {
                    return nanoseconds / 1000000; //60^2
                },
                secondToHour: function (seconds) {
                    return seconds / 3600; //60^2
                },
                byteToGigabyte: function (bytes) {
                    return bytes / 1073741824; //1024^3
                },
                toPercent: function (number) {
                    return number * 100;
                },
                timeUnitToShortTimeUnit: function (time_unit) {
                    switch (time_unit) {
                        case "NANOSECONDS":
                            return "ns";
                        case "MICROSECONDS":
                            return "micros";
                        case "MILLISECONDS":
                            return "ms";
                        case "SECONDS":
                            return "s";
                        case "MINUTES":
                            return "mins";
                        case "HOURS":
                            return "hrs";
                        case "DAYS":
                            return "days";
                        default:
                            return time_unit;
                    }
                },
                durationToString: function (duration) {

                    var time_unit = duration.timeUnit.toUpperCase();
                    var length = duration.length;

                    if (length == 9223372036854775807) {
                        return "Infinite"
                    }
                    if (length == 0) {
                        return "None"
                    }

                    return length + " " + this.timeUnitToShortTimeUnit(time_unit);
                },
                intervalsToString: function (intervals) {

                    if (intervals.name == "OscillatingExponentialInterval") {
                        return "Oscillating Exponential(min: " + this.durationToString(intervals.minMean) + ", max: " + this.durationToString(intervals.maxMean) + ", cycle: " + this.durationToString(intervals.cycleLength) + ")"
                    }

                    if (intervals.name == "FixedExponentialInterval") {
                        return "Exponential(mean: " + this.durationToString(intervals.mean) + ")"
                    }
                    if (intervals.name == "ConstantIntervalGenerator") {
                        return this.durationToString(intervals.constantInterval);
                    }

                    return JSON.stringify(intervals);
                },
                workloadToString: function (workload) {
                    var intervals = this.intervalsToString(workload.intervals);
                    return  intervals == "Infinite" ? "None" : intervals;
                },
                churnToString: function (churn) {
                    return "session length: " + this.intervalsToString(churn.sessionLength) +
                        ", down time: " + this.intervalsToString(churn.downtime);
                },
                maintenanceToString: function (maintenance, scenario) {

                    if (maintenance.name == "Maintenance") {

                        var strategy = maintenance.strategy;
                        if (strategy == undefined) {
                            return "None";
                        }
                        return strategy.name
                    }

                    if (maintenance.name == "EvolutionaryMaintenance") {

                        return "Evolutionary(population size: " +
                            maintenance.populationSize +
                            ", elite count: " +
                            maintenance.eliteCount +
                            ", mutation prob.: " +
                            maintenance.mutationProbability +
                            ", trial length: " +
                            this.durationToString({timeUnit: maintenance.evolutionCycleLengthUnit, length: maintenance.evolutionCycleLength}) +
                            ", clusterer: " +
                            ( maintenance.clustererName === undefined ? "None[small-space]" : maintenance.clustererName == 'PFClustClusterer' && scenario.name.match(/scenario_batch(4|5|6|7|8|9)_.*/) != null ? maintenance.clustererName + 'Optimised' : maintenance.clustererName)
                            + ")";
                    }
                    if (maintenance.name == "RandomMaintenance") {

                        return "RandomSearch(population size: " +
                            maintenance.populationSize +
                            ", trial length: " +
                            this.durationToString({timeUnit: maintenance.evolutionCycleLengthUnit, length: maintenance.evolutionCycleLength}) +
                            ", clusterer: " +
                            ( maintenance.clustererName === undefined ? "OLD" : maintenance.clustererName)
                            + ")";
                    }

                    return JSON.stringify(maintenance);
                }
            },
            read: function (url) {

                var request = new XMLHttpRequest();
                request.open('GET', url, false);
                request.send();

                if (request.status == 200) {
                    return request.responseText;
                } else {
                    console.log('failed to load ' + url);
                    return '';
                }
            },
            readAsJSON: function (url) {
                var content = this.read(url);
                return $.parseJSON(content);
            },

            resultsPath: '../../../../../results/',
            scenarioJSONPath: function (scenario_name) {
                return this.resultsPath + scenario_name + '/scenario.json';
            },
            repetitionsJSONPath: function (scenario_name) {
                return this.resultsPath + scenario_name + '/repetitions/repetitions.json';
            },

            analysisPath: function (scenario_name, file_name) {
                return this.resultsPath + scenario_name + "/analysis/" + file_name;
            },
            resizeElementHeight: function (element) {
                var height = 0;
                var body = window.document.body;
                if (window.innerHeight) {
                    height = window.innerHeight;
                } else if (body.parentElement.clientHeight) {
                    height = body.parentElement.clientHeight;
                } else if (body && body.clientHeight) {
                    height = body.clientHeight;
                }
                element.style.height = ((height - element.offsetTop - 50) + "px");
            },
            euclideanDistance: function (p1, p2) {

                if (p1.length != p2.length) {
                    throw  "unequal pint lengths"
                }

                var sum = 0;
                for (var i = 0; i < p1.length; i++) {
                    var dp = p1[i] - p2[i];
                    sum += dp * dp;
                }
                return Math.sqrt(sum);
            }
        }
    });