AJS.$(function ($) {

    // Event handler to prepare upload
    var files = [];
    $('#reqif-integration-upload-form').find('input[type=file]').on('change', function (event) {
        files = event.target.files;
    });

    $('.field-group').find('input[type=file]').on('change', function (event) {
        files = event.target.files;
    });

    var pageId;
    var $attachmentId = $("#attachmentId");
    var attachmentId = $attachmentId.val();
    var $attachmentDestinationPage = $("#attachment-destination-page");
    var DEFAULT_LABEL =  "ry-reqif-import";
    var MAPPED_ONCE_FIELDS = ['KEY', 'DESCRIPTION'];

    // if attachment exists for the current reqif file
    if (attachmentId) {
        // then the attachment destination page is also available
        pageId = $attachmentDestinationPage.val();
    }

    // Sets the page id when user selects a page in the given list
    $("#attachment-destination-page").on("selected.autocomplete-content", function (event, data) {
        // The selected page is stored in data
        pageId = data.content.id;

    });

    // When a user start typing in the field
    $("#attachment-destination-page").on("open.autocomplete-content", function (event, data) {
        pageId = null;
    });

    $("#reqif-integration-import-action").on("click", function(e) {
        e.preventDefault();
        importFile();
        return false;
    });

    $("#reqif-integration-reimport-action").on("click", function(e) {
        e.preventDefault();
        importFile();
        return false;
    });

    function importFile() {
        var hasError = false;
        if (!files || files.size == 0) {
            hasError = showErrorMessage('No file was attached.');
        }
        if (!pageId) {
            hasError = showErrorMessage('Please tell us which page you want to attach it to.');
        }
        if (hasError) {
            return;
        }
        var fileToUpload = files[0];
        var filename = fileToUpload.name;
        var path;
        var extractAttachmentId;

        if (attachmentId) {
            // the attachment already exists
            path = AJS.contextPath() + "/rest/api/content/" + pageId + "/child/attachment/" + attachmentId + "/data";
            extractAttachmentId = function(data) {
                // extract attachment from server response
                // Old attachments started with 'att'
                var id = data.id;
                if (id.indexOf("att") == 0) {
                    id = id.substr(3);
                }
                return id;
            };
            filename = $("#filename").val();
        } else {
            // the attachment doesn't exist
            path = AJS.contextPath() + "/rest/api/content/" + pageId + "/child/attachment";
            extractAttachmentId = function (data) {
                // extract attachment from server response
                // Old attachments started with 'att'
                var id = data.results[0].id;
                if (id.indexOf("att") == 0) {
                    id = id.substr(3);
                }
                return id;
            };
        }

        var actionData = new FormData();
        actionData.append('file', fileToUpload, filename);
        actionData.append('minorEdit', 'true');
        startUploadAnimation();
        $.ajax({
            type: 'POST',
            url: path,
            data: actionData,
            processData: false,
            headers: {
                "X-Atlassian-Token": "no-check"
            },
            contentType: false,
            cache: false,
            success: function (data, status, response) {
                // Old attachments started with 'att'
                attachmentId = extractAttachmentId(data);
                // add label here
                $.ajax({
                    url: AJS.contextPath() + "/rest/api/content/" + attachmentId + "/label",
                    type: 'POST',
                    data: '[{"name" : "'+ DEFAULT_LABEL +'"}]',
                    contentType: 'application/json; charset=utf-8',
                    success: function (response) {
                        AJS.flag({
                            type: 'success',
                            title: 'File successfully uploaded, redirecting ...'
                        });
                        var attachmentDetailsUrl = AJS.contextPath() + "/reqif/view.action?" + $.param({
                                key: AJS.Meta.get("space-key"),
                                id: attachmentId
                            });
                        window.location.replace(attachmentDetailsUrl);
                    },
                    error: function (jqXHR) {
                        AJS.flag({
                            type: 'error',
                            title: 'Error while sending request to the server',
                            body: RY.extractMessageFromXHR(jqXHR, 300)
                        });
                        stopUploadAnimation();
                    }
                });
            },
            error: function (jqXHR) {
                AJS.flag({
                    type: 'error',
                    title: 'Error while sending request to the server',
                    body: RY.extractMessageFromXHR(jqXHR, 300)
                });
                stopUploadAnimation();
            }
        });
    }

    // trigger change eventAJS.$(function ($) {K
    $(".integration-config-column-mapping").each(function () {
        disableOption($(this).closest(".reqif-object-data"));
    })

    $(".integration-config-column-mapping").on("change", function (event) {
        // selected value of a column mapping dropdown
        var mapping = $(this).val();

        // show/hide fields to set property name
        var $propertyValue = $(this).next(".integration-config-column-mapping-value");
        if (mapping === "PROPERTY") {
            $propertyValue.removeClass("hidden");
            $propertyValue.focus();
            if (!$propertyValue.val()) {
                var nthChildIndex = $propertyValue.closest("th").index() + 1;
                var suggestedValue = $propertyValue.closest("table").find("tbody tr:first-child th:nth-child(" + nthChildIndex + ")").text().trim();
                if (suggestedValue) {
                    $propertyValue.val(suggestedValue);
                }
            }
        } else {
            $propertyValue.addClass("hidden");
            // also hide everything else related to the field (required icon)
            $propertyValue.next(".aui-iconfont-error").addClass("hidden");
        }

        disableOption($(this).closest(".reqif-object-data"));
    });

    $(".reqif-spec-object-type-is-imported").on("change", function() {
        var $field = $(this);
        var value = this.checked;
        var $parent = $field.closest(".reqif-spec-object-type");
        var $allFieldsExceptImport = $parent.find("input, select").not(".reqif-spec-object-type-is-imported, .reqif-spec-object-type-is-imported .aui-toggle-input");
        if (value) {
            $allFieldsExceptImport.attr("disabled", false);
        } else {
            $allFieldsExceptImport.attr("disabled", true);
        }
    });
    $(".reqif-column-mapping").on("change", function() {
        var $field = $(this);
        var value = $field.val();
        if (value == "PROPERTY") {
            $field.closest("th").find("input").removeClass("hidden");
        } else {
            $field.closest("th").find("input").addClass("hidden");
        }
    });

    $("form button[type=submit]").click(function() {
        // We need to perform this action so that onSubmit can know which button was clicked
        $("form button[type=submit]").removeClass("clicked");
        $(this).addClass("clicked");
    });

    var $form =  $("#reqif-integration-form").on("submit", function(e) {

        // Skip if the user is deleting the document, removing the label, etc.
        if ($("form button[type=submit].clicked").attr("value") != "save") {
            // We only gather the reqifConfigJson when the user saves it.
            startUploadAnimation();
            return;
        }

        var hasErrors = false;
        function rejectField($field, reason) {
            e.preventDefault();
            hasErrors = true;
            $field.parent().find(".aui-iconfont-error").removeClass("hidden");
            AJS.flag({
                type: 'error',
                title: "Invalid field",
                body: AJS.escapeHtml(reason),
                close: "auto"
            });
            return false; // Returning false prevents submitting the form.
        }
        $(this).find(".aui-iconfont-error").addClass("hidden");

        // For each imported part, create a SpecObjectMapping record
        var mappings = [];
        $(".reqif-spec-object-type").each(function() {
            var $div = $(this);
            var mapping = {
                identifier: $div.attr("data-identifier"),
                imported: $div.find(".reqif-spec-object-type-is-imported")[0].checked,
                category: $div.find(".reqif-spec-object-category").val(),
                mappings: []
            };
            if (mapping.imported) {
                var foundKey;
                $div.find(".reqif-column-mapping").each(function () {
                    var $dropdown = $(this);
                    var val = $dropdown.val();
                    var identifier = $dropdown.closest("th").attr("data-identifier");
                    if (val) {
                        if (val === "PROPERTY") {
                            var propertyName = $dropdown.closest("th").find(".reqif-column-mapping-property").val();
                            if (!propertyName) {
                                return rejectField($dropdown, "Property names are required");
                            }
                            val = "@" + propertyName;
                        } else if (val !== "KEY" && val !== "TEXT") {
                            return rejectField($dropdown, "Invalid mapping: " + val);
                        } else if (val === "KEY") {
                            if (!foundKey) foundKey = true;
                            else rejectField($dropdown, "Duplicate mapping: KEY");
                        }
                        mapping.mappings.push({
                            identifier: identifier,
                            target: val
                        });
                    }
                });
            }
            mappings.push(mapping);
        });
        if (hasErrors) return false;

        var reqifConfig = {
            mappings: mappings
        };

        $("#reqifConfigJson").val(JSON.stringify(reqifConfig));
        // We let the form being submitted
        startUploadAnimation();
    });

    var toggleDeleteOptions = true;
    $(".open-deletion-options").on("click", function(e) {
        //$(".open-deletion-options").toggleClass("hidden");
        if (toggleDeleteOptions) {
            $(".open-deletion-options").css("opacity", 0);
            $("#deletion-options").css("max-width", "600px");
        } else {
            $(".open-deletion-options").css("opacity", 1);
            $("#deletion-options").css("max-width", "0");
        }
        toggleDeleteOptions =! toggleDeleteOptions;
        return false;
    });

    function startUploadAnimation() {
        $(".buttons-area aui-spinner").removeClass("hidden");
    }

    function stopUploadAnimation() {
        $(".buttons-area aui-spinner").addClass("hidden");
    }

    function showErrorMessage(message) {
        AJS.flag({
            type: 'error',
            title: message,
            close: "auto"
        });
        return true;
    }
});
