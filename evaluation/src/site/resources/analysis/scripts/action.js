define(['query', 'scope'], function (query, scope) {

    $(document).ready(function () {
        $('#data_type_chooser input').on('change', function () {
            scope.data_type = $('input[name="data_type"]:checked', '#data_type_chooser').val();

            query.update();
        });
    });

    return{
        metricClicked: function (index) {
            query.metric = index;
            query.update();
        },

        filterChecked: function (checkbox, index, property) {

            var selection = query[property];
            var element_index = selection.indexOf(index);
            if (!checkbox.checked) {
                selection.removeIfExists(index);
            } else {
                selection.addIfAbsent(index);
            }
            query.update();
        }
    }

})