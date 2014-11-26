define(['theme', 'util'], function (theme, util) {
    var customisations = {
        event_completion_rate: {
            title: "Event Completion Rate",
            yAxis: {
                title: {
                    text: "Number of Events Executed Per Second"
                }
            }

        },
        lookup_correctness_ratio: {
            title: "Percentage of Unsuccessful Lookups",
            convert: {
                y: function (value) {
                    return util.convert.toPercent(1 - value);
                }
            },
            yAxis: {
                title: {
                    text: "Percentage of Unsuccessful Lookups",
                },
                min: 0,
                max: 100
            }
        },
        available_peer_counter: {
            title: "Network Size",
            yAxis: {
                title: {
                    text: "Number of Available Peers"
                }
            }
        },
        event_execution_duration_timer: {
            title: "Event Execution Duration Timer",

            yAxis: {
                title: {
                    text: "Mean Event Execution Completion Time (ms)"
                }
            }
        },
        event_execution_lag_sampler: {
            title: "Event Execution Lag",

            yAxis: {
                title: {
                    text: "Event Execution Lag (ms)"
                }
            }
        },
        event_executor_queue_size: {
            title: "Event Executor Queue Size",
            yAxis: {
                title: {
                    text: "Number of Events Queued by Executor"
                }
            }
        },
        event_scheduling_rate: {
            title: "Event Scheduling Rate",
            yAxis: {
                title: {
                    text: "Number of Events Scheduled for Execution"
                }
            }

        },
        join_failure_rate: {
            title: "Join Failure Rate",
            yAxis: {
                title: {
                    text: "Number of Failed Joins"
                }
            }
        },
        join_success_rate: {
            title: "Join Success Rate",
            yAxis: {
                title: {
                    text: "Number of Successful Joins"
                }
            }
        },
        lookup_correctness_delay_timer: {
            title: "Lookup Correctness Delay",
            convert: {
                y: util.convert.nanosecondToMillisecond
            },
            yAxis: {
                title: {
                    text: "Mean Correct Lookup Delay (ms)"
                }
            }
        },
        lookup_correctness_hop_count_sampler: {
            title: "Lookup Correctness Hop Count",
            yAxis: {
                title: {
                    text: "Mean Correct Lookup Hop Count"
                }
            }
        },
        lookup_correctness_rate: {
            title: "Lookup Correctness Rate",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Correct Lookups Per Second"
                }
            }
        },
        lookup_correctness_retry_count_sampler: {
            title: "Lookup Correctness Retry Count",
            yAxis: {
                title: {
                    text: "Mean Correct Lookup Retry Count"
                }
            }
        },
        lookup_execution_rate: {
            title: "Lookup Execution Rate",
            yAxis: {
                title: {
                    text: "Number of Lookups Executed Per Second"
                }
            }
        },
        lookup_failure_delay_timer: {
            title: "Lookup Failure Delay",
            yAxis: {
                title: {
                    text: "Mean Failed Lookup Delay (ms)"
                }
            }
        },
        lookup_failure_hop_count_sampler: {
            title: "Lookup Failure Hop Count",
            yAxis: {
                title: {
                    text: "Mean Failed Lookup Hop Count"
                }
            }
        },
        lookup_failure_rate: {
            title: "Lookup Failure Rate",
            yAxis: {
                title: {
                    text: "Number of Failed Lookups per Second"
                }
            }
        },
        lookup_failure_retry_count_sampler: {
            title: "Lookup Failure Retry Count",
            yAxis: {
                title: {
                    text: "Mean Failed Lookup Retry Count"
                }
            }
        },
        lookup_incorrectness_delay_timer: {
            title: "Lookup Incorrectness Delay Timer",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Mean Incorrect Lookup Delay (ms)"
                }
            }
        },
        lookup_incorrectness_hop_count_sampler: {
            title: "Lookup Incorrectness hop Count Sampler",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Mean Incorrect Lookup Hop Count"
                }
            }
        },
        lookup_incorrectness_rate: {
            title: "Lookup Incorrectness Rate",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Incorrect Lookups Per Second"
                }
            }
        },
        lookup_incorrectness_retry_count_sampler: {
            title: "Lookup Incorrectness Retry Count Sampler",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Mean Incorrect Lookup Retry Count"
                }
            }
        },
        memory_usage_gauge: {
            title: "JVM Heap + Non-heap Memory Usage",
            convert: {
                y: util.convert.byteToGigabyte
            },

            yAxis: {
                title: {
                    text: "Heap and Non-heap Memory usage (GB)"
                }
            }
        },
        peer_arrival_rate: {
            title: "Peer Arrival Rate",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Peers Arrived Per Second"
                }
            }
        },
        peer_departure_rate: {
            title: "Peer Departure Rate",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Peers Departed Per Second"
                }
            }
        },
        queue_size_gauge: {
            title: "Generated Events Queue Size",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Generated Events"
                }
            }
        },
        reachable_state_size_per_alive_peer_gauge: {
            title: "State Size: Reachable per Alive Peer",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Reachable State Per Alive Peer"
                }
            }
        },
        sent_bytes_rate: {
            title: "Sent Bytes Rate",
            yAxis: {
                title: {
                    text: "Bytes Sent Per Second"
                }
            }
        },
        sent_bytes_per_alive_peer_per_second_gauge: {
            title: "Sent Bytes Per Alive Peer Per Second",
            yAxis: {
                title: {
                    text: "Bytes Sent Per Alive Peer Per Second"
                }
            }
        },
        rpc_error_rate: {
            title: "RPC Error Rate Per Second",
            yAxis: {
                title: {
                    text: "RPC Error Rate Per Second"
                }
            }
        },
        reconfiguration_rate: {
            title: "Reconfiguration Rate Per Second",
            yAxis: {
                title: {
                    text: "Reconfiguration Rate Per Second"
                }
            }
        },
        system_load_average_gauge: {
            title: "JVM System Load Average Over One Minute",
            yAxis: {
                title: {
                    text: "Load Average Over One Minute"
                }
            }
        },
        thread_count_gauge: {
            title: "JVM Alive Thread Count",
            chart: {
                type: "line"
            },
            yAxis: {
                title: {
                    text: "Number of Alive Threads"
                }
            }
        },
        thread_cpu_usage_gauge: {
            title: "JVM Thread CPU Usage",

            convert: {
                y: util.convert.toPercent
            },

            yAxis: {
                title: {
                    text: "% of CPU Time Consumed By Threads"
                },
                min: 0,
                max: 100
            }
        },
        state_correctness_counter: {
            title: "Routing State Correctness",

            convert: {
                y: util.convert.toPercent
            },

            yAxis: {
                title: {
                    text: "% of Correct Routing Entries"
                },
                min: 0,
                max: 100
            }
        },
        state_completeness_counter: {
            title: "Routing State Completeness",

            convert: {
                y: util.convert.toPercent
            },

            yAxis: {
                title: {
                    text: "% of Routing State Completeness"
                },
                min: 0,
                max: 100
            }
        },

        gc_cpu_usage_gauge: {
            title: "JVM GC CPU Usage",
            convert: {
                y: util.convert.toPercent
            },


            yAxis: {
                title: {
                    text: "% of CPU Time Consumed By GC"
                },
                min: 0
            }
        },
        unreachable_state_size_per_alive_peer_gauge: {
            title: "State Size: Unreachable per Alive Peer",
            yAxis: {
                title: {
                    text: "Number of Unreachable State Per Alive Peer"
                }
            }
        },
        evolutionary_maintenance_cluster_count_sampler: {
            title: "Evolutionary Maintenance: Number of Environment Clusters",
            yAxis: {
                title: {
                    text: "Number of Environment Clusters"
                }
            }
        },
        evolutionary_maintenance_cluster_size_sampler: {
            title: "Evolutionary Maintenance: Size of Environment Clusters",
            yAxis: {
                title: {
                    text: "Mean Size of Environment Clusters"
                }
            }
        },
        strategy_action_size_sampler: {
            title: "Evolutionary Maintenance: Strategy Action Size",
            yAxis: {
                title: {
                    text: "Mean Strategy Action Size"
                }
            }
        },
        strategy_uniformity_sampler: {
            title: "Evolutionary Maintenance: Strategy Uniformity",
            yAxis: {
                title: {
                    text: "Mean Number of identical strategies across peers"
                }
            }
        },
        generated_strategy_uniformity_sampler: {
            title: "Evolutionary Maintenance: Generated Strategy Uniformity",
            yAxis: {
                title: {
                    text: "Mean Number of identical generated strategies across peers"
                }
            }
        },
        evolutionary_maintenance_fitness_sampler: {
            title: "Evolutionary Maintenance: Fitness",
            yAxis: {
                title: {
                    text: "Fitness"
                }
            }
        },
        evolutionary_maintenance_normalized_fitness_sampler: {
            title: "Evolutionary Maintenance: Normalised Fitness",
            yAxis: {
                title: {
                    text: "Normalised Fitness"
                }
            }
        },
        evolutionary_maintenance_weighted_fitness_sampler: {
            title: "Evolutionary Maintenance: Weighted Fitness",
            yAxis: {
                title: {
                    text: "Weighted Fitness"
                }
            }
        }
    }

    var extended_themes = {}

    Object.keys(customisations).forEach(function (key, index) {
        extended_themes[key] = theme.extend(customisations[key]);
    })

    return extended_themes;
});