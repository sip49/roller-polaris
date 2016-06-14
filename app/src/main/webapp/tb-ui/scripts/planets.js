$(function() {
   $.ajax({
      type: "GET",
      url: contextPath + '/tb-ui/admin/rest/planets',
      success: function(data, textStatus, xhr) {
        var tmpl = $.templates('#tableTemplate');
        var html = tmpl.render(data);
        $("#tableBody").html(html);
        $(".rollertable tr").removeClass("altrow").filter(":even").addClass("altrow");
      }
   });
   $("#confirm-delete").dialog({
      autoOpen: false,
      resizable: true,
      height: 200,
      modal: true,
      buttons: [
         {
            text: msg.confirmLabel,
            click: function() {
              var idToRemove = $(this).data('actionId');
              $.ajax({
                 type: "DELETE",
                 url: contextPath + '/tb-ui/admin/rest/planets/' + idToRemove,
                 success: function(data, textStatus, xhr) {
                    $('#' + idToRemove).remove();
                    $(".rollertable tr").removeClass("altrow").filter(":even").addClass("altrow");
                 }
              });
              $(this).dialog("close");
            }
         },
         {
            text: msg.cancelLabel,
            click: function() {
               $(this).dialog("close");
            }
         }
      ]
   });
   $("#tableBody").on('click', '.delete-link', function(e) {
      e.preventDefault();
      var tr = $(this).closest('tr');
      var actionId = tr.attr('id');
      var itemName = tr.find('td.title-cell').text();
      $('#confirm-delete')
          .dialog('option', 'title', itemName)
          .data('actionId', actionId).dialog('open');
   });
});