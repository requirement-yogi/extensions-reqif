package com.requirementyogi.extensions.reqif;

/*-
 * #%L
 * Play SQL - ReqIF Import
 * %%
 * Copyright (C) 2019 Play SQL S.A.S.U.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import bucket.core.persistence.hibernate.HibernateHandle;
import com.atlassian.bonnie.Handle;
import com.atlassian.confluence.compat.struts2.servletactioncontext.ServletActionContextCompatManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.ContextPathHolder;
import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.search.service.ContentTypeEnum;
import com.atlassian.confluence.search.v2.*;
import com.atlassian.confluence.search.v2.query.*;
import com.atlassian.confluence.search.v2.sort.ModifiedSort;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.spaces.actions.AbstractSpaceAction;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.HtmlUtil;
import com.atlassian.extras.common.log.Logger;
import com.atlassian.extras.common.log.Logger.Log;
import com.atlassian.xwork.XsrfTokenGenerator;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.playsql.requirementyogi.api.RYSettingsAPI;
import com.playsql.requirementyogi.api.RYWebInterfaceAPI;
import com.playsql.requirementyogi.api.documentimporter.ImportResults;
import com.playsql.requirementyogi.api.permissions.PermissionException;
import com.requirementyogi.server.utils.confluence.search.CQLSearchService;
import com.requirementyogi.extensions.reqif.managers.ReqifDocumentManager;
import com.requirementyogi.extensions.reqif.ui.UIReqifDocument;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig;
import com.requirementyogi.server.utils.confluence.compat.CompatibilityLayer;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.requirementyogi.extensions.reqif.managers.ReqifDocumentManager.*;
import static com.requirementyogi.server.utils.confluence.search.CQLSearchService.ONLY_ONE_PAGE;
import static org.apache.commons.lang3.ArrayUtils.contains;

public class ReqifAction extends AbstractSpaceAction {

    private static final Log log = Logger.getInstance(ReqifAction.class);

    private SearchManager searchManager;
    private AttachmentManager attachmentManager;
    private PermissionManager permissionManager;
    private RYSettingsAPI rySettingsAPI;
    private RYWebInterfaceAPI ryWebInterfaceAPI;
    private ContextPathHolder contextPathHolder;
    private ReqifDocumentManager reqifDocumentManager;
    private XsrfTokenGenerator xsrf;
    private ServletActionContextCompatManager servletActionContextCompatManager;

    /* The ID of the ReqIF attachment */
    private Long id;
    private Long attachmentPageId;
    private String message;

    /* The requirement we focus on, if there is one */
    private String focus;

    private List<Map<String, Object>> reqifAttachments = Lists.newArrayList();

    private boolean hasEditPermission;
    private Attachment attachment;
    private UIReqifDocument reqifDocument;

    private String seeAttachmentsActionUrlPath;
    private String attachmentsUrlPath;

    private Integer paginationCount;
    private Integer offset;
    private Integer limit;

    private String action;
    private String atl_token;

    private int totalResults;
    private boolean hasPreviousPage;
    private boolean hasNextPage;
    private String previousPageUrl;
    private String nextPageUrl;

    private String reqifConfigJson;
    private ReqifConfig reqifConfig;


    public String doDefault() {
        if (id != null) {
            return config();
        } else {
            return list();
        }
    }

    public String list() {
        if (getSpaceKey() == null) {
            message = "We don't know what space we're in, so we can't display this form. Please specify a space key in the URL: view.action?key=...";
            return ERROR;
        }

        // This part acquires the list of files
        searchForReqifAttachments(getSpaceKey());

        return "show-attachment-list";
    }

    public String config() {

        ConfluenceUser user = AuthenticatedUserThreadLocal.get();

        if (id == null) {
            message = "The page can't be displayed.";
            addActionError("Please provide an id=... in the URL.");
            return ERROR;
        }

        attachment = attachmentManager.getAttachment(id);
        if (attachment == null) {
            message = "The page can't be displayed.";
            addActionError("The attachment " + id + " doesn't exist.");
            return list();
        }
        if (!permissionManager.hasPermission(user, Permission.VIEW, attachment)) {
            message = "The page can't be displayed.";
            addActionError("You don't have the permissions to view attachment " + attachment.getId());
            return list();
        }
        hasEditPermission = permissionManager.hasPermission(user, Permission.EDIT, attachment);
        boolean hasLabel = reqifDocumentManager.hasLabel(attachment);
        if (!hasLabel) {
            message = "The page can't be displayed.";
            addActionError("This attachment isn't managed by the ReqIF integration. If you want to, add the label '" + ATTACHMENT_LABEL + "' to it.");
            return list();
        }

        ContentEntityObject container = attachment.getContainer();
        if (container != null) {
            String pageTitle = container.getDisplayTitle();
            attachmentPageId = container.getId();

            if (!permissionManager.hasPermission(AuthenticatedUserThreadLocal.get(), Permission.VIEW, attachment.getContainer())) {
                message = "The page can't be displayed.";
                addActionError("You don't have sufficient permissions to view the attachment '" + attachment.getDisplayTitle() + "' of page '" + pageTitle + "'.");
                return list();
            }
        } else {
            if (!permissionManager.hasPermission(user, Permission.ADMINISTER, attachment.getSpace())) {
                message = "The page can't be displayed.";
                addActionError("This attachment is not on a page. You need to be a space administrator.");
                return list();
            }
        }

        if (reqifConfigJson != null) {
            reqifConfig = ReqifUtils.GSON.fromJson(reqifConfigJson, ReqifConfig.class);
        }

        String result = performAction((action) -> {
            switch (action) {
                case "save": {
                    if (reqifConfig == null) {
                        addActionError("No configuration was received by the server for this file.");
                        return null;
                    }
                    reqifDocumentManager.saveReqifConfig(getSpaceKey(), attachment.getId(), reqifConfig);
                    ImportResults importResults = null;
                    try {
                        importResults = reqifDocumentManager.importDocument(attachment, reqifConfig, user);
                    } catch (ParseException e) {
                        log.debug("Error while importing the requirements of document " + attachment.getId(), e);
                        addActionError("Error while importing the requirements: " + e.getMessage());
                        return ERROR;
                    } catch (PermissionException e) {
                        addActionError("Permission error: " + e.getMessage());
                        return ERROR;
                    }
                    List<List<String>> messages = importResults.getMessageBeanAsText();
                    if (messages.size() != 4)
                        throw new RuntimeException("Error: The API isn't conform to specifications: " + messages.size());
                    if (messages.get(0) != null) for (String message : messages.get(0)) addActionMessage(message); // Success
                    if (messages.get(1) != null) for (String message : messages.get(1)) addActionMessage(message); // Info
                    if (messages.get(2) != null) for (String message : messages.get(2)) addActionMessage("Warning: " + message); // Warning
                    if (messages.get(3) != null) for (String message : messages.get(3)) addActionError(message); // Error
                    return null;
                }
                case "delete-requirements-and-document": {
                    reqifDocumentManager.deleteDocument(attachment, true, true, true, user);
                    addActionMessage("The attachment and requirements were removed.");
                    return list();
                }
                case "delete-requirements-only": {
                    reqifDocumentManager.deleteDocument(attachment, false, false, true, user);
                    addActionMessage("The requirements were removed. The attachment is left on the page.");
                    return list();
                }
                case "delete-document-only": {
                    reqifDocumentManager.deleteDocument(attachment, true, false, false, user);
                    addActionMessage("The attachment was removed. The requirements were left in the database.");
                    return list();
                }
                case "delete-label-only": {
                    reqifDocumentManager.deleteDocument(attachment, false, true, false, user);
                    addActionMessage("The attachment was detached by removing its label. The requirements and the attachment coexist but are not linked anymore.");
                    return list();
                }
            }
            return null;
        });
        if (result != null) return result;

        if (reqifConfig == null || reqifConfigJson == null) {
            reqifConfig = reqifDocumentManager.getReqifConfig(getSpaceKey(), attachment.getId());
            reqifConfigJson = ReqifUtils.GSON.toJson(reqifConfig);
        }
        InputStream attachmentData = attachmentManager.getAttachmentData(attachment);
        try {
            // We may already have parsed in importDocument(), but we've parsed with a different scope
            reqifDocument = reqifDocumentManager.parse(attachmentData);
        } catch (ParseException e) {
            addActionError(e.getMessage());
            reqifDocument = null;
        } finally {
            ReqifUtils.closeSilently(attachmentData);
        }

        String baseUrl = contextPathHolder.getContextPath();
        seeAttachmentsActionUrlPath = new StringBuilder(baseUrl).append("/requirementyogi/list.action")
                .append("?key=").append(HtmlUtil.urlEncode(getSpaceKey()))
                .append("&includeDeleted=false")
                .append("&queryString=").append(HtmlUtil.urlEncode("imported = '" + attachment.getIdAsString() + "'"))
                .toString();
        attachmentsUrlPath = baseUrl + container.getAttachmentsUrlPath();

        return "show-one-attachment";
    }

    private <T> T performAction(Consumer<String> callback) {
        return performAction(action -> {
            callback.accept(action);
            return null;
        });
    }

    private <T> T performAction(Function<String, T> callback) {
        if (action != null) {
            if (StringUtils.isNotBlank(atl_token) && xsrf.validateToken(CompatibilityLayer.getRequest(servletActionContextCompatManager), atl_token)) {
                return callback.apply(action);
            } else {
                addActionError("The XSRF token is invalid. That means a form was submitted, probably from another website, "
                    + "with the intent of modifying data. If you just let too much time passing, just try submitting the form again.");
            }
        }

        return null;
    }

    /**
     * Get data to do a PaginatedResults in which we sort what we really need
     *
     * @param spaceKey
     */
    public void searchForReqifAttachments(String spaceKey) {
        int currentOffset = firstNonNull(offset, 0);
        int currentLimit = firstNonNull(limit, 15);
        boolean isLastPage = false;
        List<Long> attachmentIds = Lists.newArrayList();

        LabelQuery labelQuery = new LabelQuery(ATTACHMENT_LABEL);

        ContentTypeQuery attachmentContentTypeQuery = new ContentTypeQuery(Sets.newHashSet(ContentTypeEnum.ATTACHMENT));

        SearchQuery authorizedExtensionsQuery = Arrays.stream(ATTACHMENT_EXTENSIONS)
                .map(ext -> (SearchQuery) new FileExtensionQuery(ext))
                .reduce(BooleanQuery::orQuery).get();

        SearchQuery searchForLabels = BooleanQuery.orQuery(labelQuery);

        SearchQuery query = BooleanQuery.andQuery(new InSpaceQuery(spaceKey), searchForLabels, authorizedExtensionsQuery, attachmentContentTypeQuery);
        SearchSort sort = new ModifiedSort(SearchSort.Order.DESCENDING); // latest created attachment first

        CQLSearchService cqlSearchService = CQLSearchService.getInstance();
        CQLSearchService.SearchResultAdaptor searchResults;
        try {
            searchResults = cqlSearchService.search(query, sort, currentOffset, currentLimit);
            attachmentIds = processSearchResults(searchResults);

            totalResults = searchResults.getUnfilteredResultsCount();
            isLastPage = searchResults.isLastPage();

            hasNextPage = totalResults != 0 && !isLastPage;
            hasPreviousPage = totalResults != 0 && currentOffset != 0;

        } catch (InvalidSearchException e) {
            throw new RuntimeException("Couldn't search for attachments with label " + ATTACHMENT_LABEL, e);
        }
        String urlPrefix = ryWebInterfaceAPI.getUrlPrefix(null);

        for (Long attachmentId : attachmentIds) {
            Attachment attachment = attachmentManager.getAttachment(attachmentId);
            if (shouldProcessAttachment(attachment) && !attachment.isDeleted()) {
                Map<String, Object> attachmentData = getAttachmentData(spaceKey, urlPrefix, urlPrefix + "/reqif/view.action?key=" + HtmlUtil.urlEncode(spaceKey), attachment);
                reqifAttachments.add(attachmentData);
            }
        }
        URIBuilder urlBuilder = null;
        try {
            urlBuilder = new URIBuilder(urlPrefix + "/reqif/view.action")
                    .addParameter("key", spaceKey)
                    .addParameter("limit", String.valueOf(currentLimit));
            if (hasPreviousPage) {
                int previousPageStart = ReqifUtils.between(0, currentOffset - currentLimit, totalResults);
                previousPageUrl = urlBuilder.setParameter("offset", String.valueOf(previousPageStart)).build().toASCIIString();
            }
            if (hasNextPage) {
                int nextPageStart = ReqifUtils.between(0, currentOffset + currentLimit, totalResults);
                nextPageUrl = urlBuilder.setParameter("offset", String.valueOf(nextPageStart)).build().toASCIIString();
            }
        } catch (URISyntaxException e) {
            // Do nothing, we'll just miss the previous/next hyperlinks
        }
    }

    /**
     * Return a list of Attachments to be displayed and sort the ones that are irrelevant
     */
    private static List<Long> processSearchResults(CQLSearchService.SearchResultAdaptor searchResults) {
        List<Long> attachmentIds = Lists.newArrayList();
        for (SearchResult item : searchResults.iterator(ONLY_ONE_PAGE, Function.identity())) {
            Handle handle = item.getHandle();
            if (handle instanceof HibernateHandle) {
                long id = ((HibernateHandle) handle).getId();
                // We need to check the handle is actually a CEO
                String className = ((HibernateHandle) handle).getClassName();
                try {
                    Class<?> clazz = Class.forName(className);
                    if (Attachment.class.isAssignableFrom(clazz)) {
                        attachmentIds.add(id);
                    }
                } catch (ClassNotFoundException e) {
                    log.debug("Couldn't pull a reference of type " + className + " that was returned by the Java Search API", e);
                }
            }
        }
        return attachmentIds;
    }

    /**
     * Building an object with the content of the Attachment
     *
     * @param spaceKey
     * @param urlPrefix
     * @param baseUrl
     * @param attachment
     * @return
     */
    private Map<String, Object> getAttachmentData(String spaceKey, String urlPrefix, String baseUrl, Attachment attachment) {
        Map<String, Object> attachmentData = Maps.newHashMap();

        attachmentData.put("id", attachment.getId());
        attachmentData.put("attachment", attachment);
        attachmentData.put("creationDate", attachment.getCreationDate() != null ? ReqifUtils.SIMPLE_DATE_FORMAT_HUMAN.format(attachment.getCreationDate()) : "");
        attachmentData.put("lastModificationDate", attachment.getLastModificationDate() != null ? ReqifUtils.SIMPLE_DATE_FORMAT_HUMAN.format(attachment.getLastModificationDate()) : null);

        String attachmentUrl = baseUrl + "&id=" + attachment.getId();

        String seeAttachmentsActionUrlPath = urlPrefix + "/requirementyogi/list.action?key=" + spaceKey + "&includeDeleted=false&queryString=reqif=" + attachment.getIdAsString();

        attachmentData.put("url", attachmentUrl);

        ContentEntityObject page = attachment.getContainer();
        if (page != null) {
            attachmentData.put("pageId", page.getId());
            attachmentData.put("pageTitle", page.getTitle());
            attachmentData.put("pageUrlPath", urlPrefix + page.getUrlPath());
            attachmentData.put("pageAttachmentsUrlPath", urlPrefix + page.getAttachmentsUrlPath());
            attachmentData.put("seeAttachmentsActionUrlPath", seeAttachmentsActionUrlPath);
        }
        return attachmentData;
    }

    /**
     * This method returns the Attachment with the given Id or an error if we can not find it
     *
     * @param attachmentId
     * @return
     */
    public Attachment getAttachmentAndVerify(Long attachmentId) {
        if (attachmentId == null) {
            addActionError("No attachment id was provided. Is there an 'id' parameter in the address bar?");
            return null;
        }

        Attachment att = attachmentManager.getAttachment(attachmentId);
        if (att == null) {
            addActionError("No attachment is known for this ID.");
            return null;
        }
        if (!shouldProcessAttachment(att)) {
            addActionError("The attachment isn't suitable for import. It must have the label '" + ATTACHMENT_LABEL + "' and one of those extensions: " + StringUtils.join(ATTACHMENT_EXTENSIONS, ", ") + ".");
            return null;
        }
        if (att.isDeleted()) {
            ContentEntityObject container = att.getContainer();
            if (container != null) {
                if (container.isDeleted()) {
                    addActionError("This attachment was deleted. It was on page '" + container.getDisplayTitle() + "', which is deleted too. Your space administrator can go to Space Tools -> Content Tools -> Trash, to restore this attachment.");
                } else {
                    addActionError("This attachment was deleted. It was on page '" + container.getDisplayTitle() + "'.");
                }
            } else {
                addActionError("This attachment was deleted. We don't know which page it was on.");
            }
            return null;
        }

        return att;
    }

    /**
     * Check if an attachment is a reqif attachment
     */
    public boolean shouldProcessAttachment(Attachment attachment) {
        if (attachment == null)
            return false;
        // Check that attachment is a ReqIf file
        if (!contains(ATTACHMENT_EXTENSIONS, attachment.getFileExtension()))
            return false;

        List<Label> attachmentLabels = attachment.getLabels();
        return attachmentLabels != null && attachmentLabels.contains(labelManager.getLabel(ATTACHMENT_LABEL));
    }

    public String getNavbarHtml(String tab) {
        return ryWebInterfaceAPI.getNavbarHtml(getSpaceKey(), tab, this);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Long getAttachmentPageId() {
        return attachmentPageId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPaginationCount() {
        return paginationCount;
    }

    public void setPaginationCount(Integer paginationCount) {
        this.paginationCount = paginationCount;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public int getTotalResults() {
        return totalResults;
    }

    public boolean isHasPreviousPage() {
        return hasPreviousPage;
    }

    public boolean isHasNextPage() {
        return hasNextPage;
    }

    public void setSearchManager(SearchManager searchManager) {
        this.searchManager = searchManager;
    }

    public void setAttachmentManager(AttachmentManager attachmentManager) {
        this.attachmentManager = attachmentManager;
    }

    public List<Map<String, Object>> getReqifAttachments() {
        return reqifAttachments;
    }

    public Integer getLimitForImport() {
        return rySettingsAPI.getLimitForImport();
    }

    public UIReqifDocument getReqifDocument() {
        return reqifDocument;
    }

    public Attachment getAttachment() {
        return attachment;
    }

    public String getSeeAttachmentsActionUrlPath() {
        return seeAttachmentsActionUrlPath;
    }

    public String getAttachmentsUrlPath() {
        return attachmentsUrlPath;
    }

    public void setContextPathHolder(ContextPathHolder contextPathHolder) {
        this.contextPathHolder = contextPathHolder;
    }

    public void setReqifConfigJson(String reqifConfigJson) {
        this.reqifConfigJson = reqifConfigJson;
    }

    public void setReqifDocumentManager(ReqifDocumentManager reqifDocumentManager) {
        this.reqifDocumentManager = reqifDocumentManager;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public String getFocus() {
        return focus;
    }

    public ReqifConfig getReqifConfig() {
        return reqifConfig;
    }

    public boolean isHasEditPermission() {
        return hasEditPermission;
    }

    public XsrfTokenGenerator getXsrf() {
        return xsrf;
    }

    public void setXsrf(XsrfTokenGenerator xsrf) {
        this.xsrf = xsrf;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getAtl_token() {
        return atl_token;
    }

    public void setAtl_token(String atl_token) {
        this.atl_token = atl_token;
    }

    public void setRyWebInterfaceAPI(RYWebInterfaceAPI ryWebInterfaceAPI) {
        this.ryWebInterfaceAPI = ryWebInterfaceAPI;
    }

    public void setRySettingsAPI(RYSettingsAPI rySettingsAPI) {
        this.rySettingsAPI = rySettingsAPI;
    }

    public String getPreviousPageUrl() {
        return previousPageUrl;
    }

    public String getNextPageUrl() {
        return nextPageUrl;
    }

    public void setServletActionContextCompatManager(ServletActionContextCompatManager servletActionContextCompatManager) {
        this.servletActionContextCompatManager = servletActionContextCompatManager;
    }

    @Override
    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }
}
