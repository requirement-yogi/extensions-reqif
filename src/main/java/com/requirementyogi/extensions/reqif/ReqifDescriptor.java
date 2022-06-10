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

import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.util.HtmlUtil;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.playsql.requirementyogi.api.beans.ImportedRef;
import com.playsql.requirementyogi.api.beans.Requirement;
import com.playsql.requirementyogi.api.documentimporter.DocumentId;
import com.playsql.requirementyogi.api.documentimporter.Document;
import com.playsql.requirementyogi.api.documentimporter.Descriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class ReqifDescriptor implements Descriptor {

    public static final String DESCRIPTOR_KEY = "reqif";
    private final ContentEntityManager ceoManager;
    private final AttachmentManager attachmentManager;
    private final ApplicationProperties applicationProperties;

    public ReqifDescriptor(ContentEntityManager ceoManager, AttachmentManager attachmentManager, ApplicationProperties applicationProperties) {
        this.ceoManager = ceoManager;
        this.attachmentManager = attachmentManager;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public String getKey() {
        return DESCRIPTOR_KEY;
    }

    @Override
    public String getDisplayName() {
        return "ReqIF 1.0";
    }

    @Override
    public String getDocumentationURL() {
        return null;
    }

    @Override
    public String getAdminURL() {
        return null;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public void fillDocumentDetails(String spaceKey, @Nullable Requirement bean, ImportedRef importedRef, Map<String, Object> stash) {
        String baseUrl = (String) stash.get("base-url");
        if (baseUrl == null) {
            baseUrl = applicationProperties.getBaseUrl(UrlMode.ABSOLUTE);
            stash.put("base-url", baseUrl);
        }
        ContentEntityObject attachment = (ContentEntityObject) stash.get("ceo-" + importedRef.getDocumentId());
        if (attachment == null) {
            attachment = ceoManager.getById(Long.parseLong(importedRef.getDocumentId()));
            stash.put("ceo-" + importedRef.getDocumentId(), attachment);
        }
        String externalUrl = baseUrl + "/" + importedRef.getDescriptorKey() + "/view.action?key=" + HtmlUtil.urlEncode(spaceKey) +
                "&id=" + importedRef.getDocumentId() +
                "&focus=" + importedRef.getMarkerInDocument();

        importedRef.set(attachment != null ? attachment.getDisplayTitle() : "ReqIF file", externalUrl);
    }

    @Override
    public Document getDocumentMetadata(@Nonnull DocumentId documentId) {
        Attachment attachment = attachmentManager.getAttachment(Long.parseLong(documentId.getId()));
        if (attachment != null) {
            return new Document(documentId, attachment.getDisplayTitle(), applicationProperties.getBaseUrl(UrlMode.ABSOLUTE) + attachment.getUrlPath());
        } else {
            return new Document(documentId, "Attachment " + documentId.getId(), null);
        }
    }
}
