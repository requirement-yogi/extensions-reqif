package com.requirementyogi.extensions.reqif.managers;

/*
 * #%L
 * Play SQL - ReqIF Import
 * %%
 * Copyright (C) 2016 - 2019 Play SQL S.A.S.U.
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

import com.atlassian.confluence.content.render.xhtml.RenderedContentCleaner;
import com.atlassian.confluence.event.events.content.attachment.AttachmentRemoveEvent;
import com.atlassian.confluence.labels.Label;
import com.atlassian.confluence.labels.LabelManager;
import com.atlassian.confluence.labels.Labelable;
import com.atlassian.confluence.pages.Attachment;
import com.atlassian.confluence.pages.AttachmentManager;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.extras.common.log.Logger;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.playsql.requirementyogi.api.DocumentImporterAPI;
import com.playsql.requirementyogi.api.documentimporter.DocumentId;
import com.playsql.requirementyogi.api.documentimporter.ImportResults;
import com.playsql.requirementyogi.api.permissions.PermissionException;
import com.requirementyogi.extensions.reqif.ReqifDescriptor;
import com.requirementyogi.extensions.reqif.ReqifUtils;
import com.requirementyogi.extensions.reqif.ui.UIReqifDocument;
import com.requirementyogi.extensions.reqif.xml.ReqifConfig;
import com.requirementyogi.extensions.reqif.xml.ReqifXmlHandler;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

public class ReqifDocumentManager implements InitializingBean, DisposableBean {
    private static final Logger.Log log = Logger.getInstance(ReqifDocumentManager.class);

    public static final String ATTACHMENT_LABEL = "ry-reqif-import";
    public static final String[] ATTACHMENT_EXTENSIONS = {"reqif"};
    public static final String SETTINGS_ROOT = "com.requirementyogi.extensions.reqif.";
    public static final String SETTINGS_ATTACHMENT = SETTINGS_ROOT + "attachment.";

    private final RenderedContentCleaner antisamy;
    private final AttachmentManager attachmentManager;
    private final PluginSettingsFactory pluginSettingsFactory;
    private final DocumentImporterAPI externalAPI;
    private final LabelManager labelManager;
    private final EventPublisher eventPublisher;

    public ReqifDocumentManager(DocumentImporterAPI externalAPI,
                                RenderedContentCleaner antisamy,
                                AttachmentManager attachmentManager,
                                PluginSettingsFactory pluginSettingsFactory,
                                LabelManager labelManager,
                                EventPublisher eventPublisher) {
        this.externalAPI = externalAPI;
        this.antisamy = antisamy;
        this.attachmentManager = attachmentManager;
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.labelManager = labelManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    /**
     * Parses the file, creates a ReqifParser document, and closes the stream in any case.
     */
    public UIReqifDocument parse(InputStream attachmentData) throws ParseException {
       try {
           UIReqifDocument reqifDocument;
           ReqifXmlHandler reqifHandler = new ReqifXmlHandler();
           SAXParserFactory saxFactory = SAXParserFactory.newInstance();
           boolean isNamespaceAware = saxFactory.isNamespaceAware();
           if (!isNamespaceAware) {
               saxFactory.setNamespaceAware(true);
           }
           SAXParser saxParser;
           try {
               saxParser = saxFactory.newSAXParser();
               saxParser.parse(attachmentData, reqifHandler);
               reqifDocument = reqifHandler.getReqifDocument(antisamy);
               return reqifDocument;
           } catch (ParserConfigurationException | SAXException | IOException e) {
               throw new ParseException("Couldn't parse this ReqIF file: " + e.getMessage(), e);
           }
       } finally {
           if (attachmentData != null) {
               try {
                   attachmentData.close();
               } catch (IOException e) {
                   // Do nothing
               }
           }
       }
    }

    public static class ParseException extends Exception {
       public ParseException() {
       }

       public ParseException(String message) {
           super(message);
       }

       public ParseException(String message, Throwable cause) {
           super(message, cause);
       }

       public ParseException(Throwable cause) {
           super(cause);
       }
   }

    public ReqifConfig getReqifConfig(String spaceKey, Long attachmentId) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(spaceKey);
        Object config = settings.get(SETTINGS_ATTACHMENT + attachmentId.toString());
        String configAsString = config instanceof String ? (String) config : null;
        if (StringUtils.isBlank(configAsString)) {
            return null;
        }

        return ReqifUtils.GSON.fromJson(configAsString, ReqifConfig.class);
    }

    public void saveReqifConfig(String spaceKey, long attachmentId, ReqifConfig config) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(spaceKey);
        settings.put(SETTINGS_ATTACHMENT + String.valueOf(attachmentId), ReqifUtils.GSON.toJson(config));
    }

    public void removeReqifConfig(String spaceKey, long attachmentId) {
        PluginSettings settings = pluginSettingsFactory.createSettingsForKey(spaceKey);
        settings.remove(SETTINGS_ATTACHMENT + String.valueOf(attachmentId));
    }

    public boolean hasLabel(Attachment attachment) {
        boolean hasLabel = false;
        for (Label label : attachment.getLabels()) {
            if (Objects.equals(label.getName(), ATTACHMENT_LABEL)) {
                hasLabel = true;
                break;
            }
        }
        return hasLabel;
    }

    @EventListener
    public void on(AttachmentRemoveEvent event) {
        Attachment attachment = event.getAttachment();
        if (attachment != null && hasLabel(attachment) && StringUtils.isNotBlank(attachment.getSpaceKey())) {
            removeReqifConfig(attachment.getSpaceKey(), attachment.getId());
        }
    }

    public ImportResults importDocument(Attachment attachment, ReqifConfig reqifConfig, ConfluenceUser user) throws ParseException, PermissionException {
        if (reqifConfig == null) throw new NullPointerException("config");
        InputStream attachmentData = attachmentManager.getAttachmentData(attachment);
        try {
            String documentTitle = attachment.getDisplayTitle();
            String documentId = attachment.getIdAsString();
            String spaceKey = attachment.getSpaceKey();
            UIReqifDocument reqifDocument = parse(attachmentData);
            return externalAPI.importDocument(new DocumentId(ReqifDescriptor.DESCRIPTOR_KEY, spaceKey, documentId), documentTitle, user, (api) -> {
                reqifDocument.importRequirements(api, reqifConfig, spaceKey, documentId);
            });
        } finally {
            ReqifUtils.closeSilently(attachmentData);
        }
    }

    /**
     * Delete a document. The 3 parts of the deletion are distinct, so callers can:
     * - Just remove the label, but leave the requirements and the attachment,
     * - Just remove the attachment (and the label), but leave the requirements,
     * - Just remove the requirements, as if the document was never imported.
     *
     * @param attachment the attachment
     * @param deleteAttachment true if the attachment must be removed
     * @param removeLabel true if the label must be removed (and automatically true if deleteAttachment or deleteRequirements is true)
     * @param deleteRequirements true if the requirements must be removed.
     */
    public void deleteDocument(Attachment attachment, boolean deleteAttachment, boolean removeLabel, boolean deleteRequirements, ConfluenceUser user) {
        if (deleteRequirements) {
            String documentTitle = attachment.getDisplayTitle();
            String documentId = attachment.getIdAsString();
            String spaceKey = attachment.getSpaceKey();
            try {
                externalAPI.importDocument(new DocumentId(ReqifDescriptor.DESCRIPTOR_KEY, spaceKey, documentId), documentTitle, user, (api) -> {
                    // Do nothing, it will delete all requirements
                });
            } catch (PermissionException e) {
                throw new RuntimeException("The user doesn't have the permissions to delete those requirements.");
            }
            removeLabel = true;
        }

        if (removeLabel) {
            Label label = labelManager.getLabel(new Label(ATTACHMENT_LABEL));
            if (label != null) {
                labelManager.removeLabel((Labelable) attachment, label);
            }
        }

        if (deleteAttachment) {
            attachmentManager.trash(attachment);
        }
    }
}
