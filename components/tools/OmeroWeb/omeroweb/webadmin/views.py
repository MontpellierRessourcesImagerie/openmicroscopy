#!/usr/bin/env python
# 
# 
# 
# Copyright (c) 2008 University of Dundee. 
# 
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
# 
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 
# Author: Aleksandra Tarkowska <A(dot)Tarkowska(at)dundee(dot)ac(dot)uk>, 2008.
# 
# Version: 1.0
#

''' A view functions is simply a Python function that takes a Web request and 
returns a Web response. This response can be the HTML contents of a Web page, 
or a redirect, or the 404 and 500 error, or an XML document, or an image... 
or anything.'''

import os
import sys
import locale
import calendar
import datetime
import traceback
import logging
import re

import omeroweb.webclient.views

from time import time

from omero_version import omero_version

from django.conf import settings
from django.contrib.sessions.backends.cache import SessionStore
from django.core import template_loader
from django.core.cache import cache
from django.core.urlresolvers import reverse
from django.http import HttpResponse, HttpRequest, HttpResponseRedirect, Http404
from django.shortcuts import render_to_response
from django.template import RequestContext as Context
from django.utils import simplejson
from django.utils.translation import ugettext as _
from django.views.defaults import page_not_found, server_error
from django.views import debug
from django.core.cache import cache
from django.utils.encoding import smart_str

from webclient.webclient_gateway import OmeroWebGateway

from forms import LoginForm, ForgottonPasswordForm, ExperimenterForm, \
                   GroupForm, GroupOwnerForm, MyAccountForm, ChangePassword, \
                   ContainedExperimentersForm, UploadPhotoForm, \
                   EnumerationEntry, EnumerationEntries

from controller import BaseController
from controller.experimenter import BaseExperimenters, BaseExperimenter
from controller.group import BaseGroups, BaseGroup
from controller.drivespace import BaseDriveSpace, usersData
from controller.uploadfile import BaseUploadFile
from controller.enums import BaseEnums

from omeroweb.webadmin.webadmin_utils import _checkVersion, _isServerOn, toBoolean, upgradeCheck, getGuestConnection

from omeroweb.webadmin.custom_models import Server

from omeroweb.webclient.decorators import login_required
from omeroweb.connector import Connector

logger = logging.getLogger(__name__)

logger.info("INIT '%s'" % os.getpid())

################################################################################
# decorators

class render_response_admin(omeroweb.webclient.decorators.render_response):
    """ Subclass for adding additional data to the 'context' dict passed to templates """

    def prepare_context(self, request, context, *args, **kwargs):
        """
        We extend the webclient render_response to check if any groups are created.
        If not, add an appropriate message to the template context
        """
        super(render_response_admin, self).prepare_context(request, context, *args, **kwargs)

        if 'info' not in context:
            context['info'] = {}
        context['info']['today'] = _("Today is %(tday)s") % {'tday': datetime.date.today()}
        context['omero_version'] = request.session.get('version')
        noGroupsCreated = kwargs["conn"].isAnythingCreated()
        if noGroupsCreated:
            msg = _('User must be in a group - You have not created any groups yet. Click <a href="%s">here</a> to create a group') % (reverse(viewname="wamanagegroupid", args=["new"]))
            context['info']['message'] = msg

################################################################################
# views controll

def forgotten_password(request, **kwargs):
    request.session.modified = True
    
    template = "webadmin/forgotten_password.html"
    
    conn = None
    error = None
    blitz = None
    
    if request.method == 'POST':
        form = ForgottonPasswordForm(data=request.REQUEST.copy())
        if form.is_valid():
            blitz = Server.get(pk=request.REQUEST.get('server'))
            try:
                conn = getGuestConnection(blitz.host, blitz.port)
                if not conn.isForgottenPasswordSet():
                    error = "This server cannot reset password. Please contact your administrator."
                    conn = None
            except Exception, x:
                logger.error(traceback.format_exc())
                error = "Internal server error, please contact administrator."
        
            if conn is not None:
                try:
                    conn.reportForgottenPassword(smart_str(request.REQUEST.get('username')), smart_str(request.REQUEST.get('email')))
                    error = "Password was reseted. Check you mailbox."
                    form = None
                except Exception, x:
                    logger.error(traceback.format_exc())
                    error = "Internal server error, please contact administrator."
    else:
        form = ForgottonPasswordForm()
    
    context = {'error':error, 'form':form}    
    t = template_loader.get_template(template)
    c = Context(request, context)
    rsp = t.render(c)
    return HttpResponse(rsp)

@login_required()
def index(request, **kwargs):
    conn = None
    try:
        conn = kwargs["conn"]
    except:
        logger.error(traceback.format_exc())
    
    if conn.isAdmin():
        return HttpResponseRedirect(reverse("waexperimenters"))
    else:
        return HttpResponseRedirect(reverse("wamyaccount"))

@login_required()
def logout(request, **kwargs):
    omeroweb.webclient.views.logout(request, **kwargs)
    return HttpResponseRedirect(reverse("waindex"))

@login_required(isAdmin=True)
@render_response_admin()
def experimenters(request, conn=None, **kwargs):
    experimenters = True
    template = "webadmin/experimenters.html"
    
    info = {'experimenters':experimenters}
    controller = BaseExperimenters(conn)
    
    context = {'nav':request.session['nav'], 'info':info, 'controller':controller}
    context['template'] = template
    return context

@login_required(isAdmin=True)
@render_response_admin()
def manage_experimenter(request, action, eid=None, conn=None, **kwargs):
    experimenters = True
    template = "webadmin/experimenter_form.html"
    
    info = {'experimenters':experimenters}
    controller = BaseExperimenter(conn, eid)
    
    if action == 'new':
        form = ExperimenterForm(initial={'with_password':True, 'active':True, 'available':controller.otherGroupsInitialList()})        
        context = {'info':info, 'form':form}
    elif action == 'create':
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamanageexperimenterid", args=["new"]))
        else:
            name_check = conn.checkOmeName(request.REQUEST.get('omename'))
            email_check = conn.checkEmail(request.REQUEST.get('email'))
            
            initial={'with_password':True}
            
            exclude = list()            
            if len(request.REQUEST.getlist('other_groups')) > 0:
                others = controller.getSelectedGroups(request.REQUEST.getlist('other_groups'))   
                initial['others'] = others
                initial['default'] = [(g.id, g.name) for g in others]
                exclude.extend([g.id for g in others])
            
            available = controller.otherGroupsInitialList(exclude)
            initial['available'] = available
            form = ExperimenterForm(initial=initial, data=request.REQUEST.copy(), name_check=name_check, email_check=email_check)
            if form.is_valid():
                logger.debug("Create experimenter form:" + str(form.cleaned_data))
                omename = form.cleaned_data['omename']
                firstName = form.cleaned_data['first_name']
                middleName = form.cleaned_data['middle_name']
                lastName = form.cleaned_data['last_name']
                email = form.cleaned_data['email']
                institution = form.cleaned_data['institution']
                admin = toBoolean(form.cleaned_data['administrator'])
                active = toBoolean(form.cleaned_data['active'])
                defaultGroup = form.cleaned_data['default_group']
                otherGroups = form.cleaned_data['other_groups']
                password = form.cleaned_data['password']
                controller.createExperimenter(omename, firstName, lastName, email, admin, active, defaultGroup, otherGroups, password, middleName, institution)
                return HttpResponseRedirect(reverse("waexperimenters"))
            context = {'info':info, 'form':form}
    elif action == 'edit' :
        initial={'omename': controller.experimenter.omeName, 'first_name':controller.experimenter.firstName,
                                'middle_name':controller.experimenter.middleName, 'last_name':controller.experimenter.lastName,
                                'email':controller.experimenter.email, 'institution':controller.experimenter.institution,
                                'administrator': controller.experimenter.isAdmin(), 'active': controller.experimenter.isActive(), 
                                'default_group': controller.defaultGroup, 'other_groups':controller.otherGroups}
        
        initial['default'] = controller.default
        others = controller.others
        initial['others'] = others
        if len(others) > 0:
            exclude = [g.id.val for g in others]
        else:
            exclude = [controller.defaultGroup]
        available = controller.otherGroupsInitialList(exclude)
        initial['available'] = available
        form = ExperimenterForm(initial=initial)
        
        context = {'info':info, 'form':form, 'eid': eid, 'ldapAuth': controller.ldapAuth}
    elif action == 'save':
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamanageexperimenterid", args=["edit", controller.experimenter.id]))
        else:            
            name_check = conn.checkOmeName(request.REQUEST.get('omename'), controller.experimenter.omeName)
            email_check = conn.checkEmail(request.REQUEST.get('email'), controller.experimenter.email)
            initial={'active':True}
            exclude = list()
            
            if len(request.REQUEST.getlist('other_groups')) > 0:
                others = controller.getSelectedGroups(request.REQUEST.getlist('other_groups'))   
                initial['others'] = others
                initial['default'] = [(g.id, g.name) for g in others]
                exclude.extend([g.id for g in others])
            
            available = controller.otherGroupsInitialList(exclude)
            initial['available'] = available
            
            form = ExperimenterForm(initial=initial, data=request.POST.copy(), name_check=name_check, email_check=email_check)
               
            if form.is_valid():
                logger.debug("Update experimenter form:" + str(form.cleaned_data))
                omename = form.cleaned_data['omename']
                firstName = form.cleaned_data['first_name']
                middleName = form.cleaned_data['middle_name']
                lastName = form.cleaned_data['last_name']
                email = form.cleaned_data['email']
                institution = form.cleaned_data['institution']
                admin = toBoolean(form.cleaned_data['administrator'])
                active = toBoolean(form.cleaned_data['active'])
                defaultGroup = form.cleaned_data['default_group']
                otherGroups = form.cleaned_data['other_groups']
                controller.updateExperimenter(omename, firstName, lastName, email, admin, active, defaultGroup, otherGroups, middleName, institution)
                return HttpResponseRedirect(reverse("waexperimenters"))
            context = {'info':info, 'form':form, 'eid': eid, 'ldapAuth': controller.ldapAuth}
    elif action == "delete":
        controller.deleteExperimenter()
        return HttpResponseRedirect(reverse("waexperimenters"))
    else:
        return HttpResponseRedirect(reverse("waexperimenters"))
    
    context['template'] = template
    return context


@login_required()
@render_response_admin()
def manage_password(request, eid, conn=None, **kwargs):
    experimenters = True
    template = "webadmin/password.html"

    info = {'experimenters':experimenters}
    
    error = None
    if request.method == 'POST':
        password_form = ChangePassword(data=request.POST.copy())
        if not password_form.is_valid():
            error = password_form.errors
        else:
            old_password = password_form.cleaned_data['old_password']
            password = password_form.cleaned_data['password']
            # if we're trying to change our own password...
            if conn.getEventContext().userId == int(eid):
                try:
                    conn.changeMyPassword(password, old_password)
                except Exception, x:
                    error = x.message   # E.g. old_password not valid
            elif conn.isAdmin():
                exp = conn.getObject("Experimenter", eid)
                try:
                    conn.changeUserPassword(exp.omeName, password, old_password)
                except Exception, x:
                    error = x.message
                else:
                    return HttpResponseRedirect(reverse(viewname="wamanageexperimenterid", args=["edit", eid]))
            else:
                raise AttributeError("Can't change another user's password unless you are an Admin")
                
    context = {'info':info, 'error':error, 'password_form':password_form, 'eid': eid}
    context['template'] = template
    return context


@login_required(isAdmin=True)
@render_response_admin()
def groups(request, conn=None, **kwargs):
    groups = True
    template = "webadmin/groups.html"
    
    info = {'groups':groups}
    
    controller = BaseGroups(conn)
    
    context = {'info':info, 'controller':controller}
    context['template'] = template
    return context


@login_required(isAdmin=True)
@render_response_admin()
def manage_group(request, action, gid=None, conn=None, **kwargs):
    groups = True
    template = "webadmin/group_form.html"
    
    info = {'groups':groups}
    controller = BaseGroup(conn, gid)
    
    if action == 'new':
        form = GroupForm(initial={'experimenters':controller.experimenters, 'permissions': 0})
        context = {'info':info, 'form':form}
    elif action == 'create':
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamanagegroupid", args=["new"]))
        else:
            name_check = conn.checkGroupName(request.REQUEST.get('name'))
            form = GroupForm(initial={'experimenters':controller.experimenters}, data=request.POST.copy(), name_check=name_check)
            if form.is_valid():
                logger.debug("Create group form:" + str(form.cleaned_data))
                name = form.cleaned_data['name']
                description = form.cleaned_data['description']
                owners = form.cleaned_data['owners']
                permissions = form.cleaned_data['permissions']
                readonly = toBoolean(form.cleaned_data['readonly'])
                controller.createGroup(name, owners, permissions, readonly, description)
                return HttpResponseRedirect(reverse("wagroups"))
            context = {'info':info, 'form':form}
    elif action == 'edit':
        permissions = controller.getActualPermissions()
        form = GroupForm(initial={'name': controller.group.name, 'description':controller.group.description,
                                     'permissions': permissions, 'readonly': controller.isReadOnly(), 
                                     'owners': controller.owners, 'experimenters':controller.experimenters})
        context = {'info':info, 'form':form, 'gid': gid, 'permissions': permissions}
    elif action == 'save':
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamanagegroupid", args=["edit", controller.group.id]))
        else:
            name_check = conn.checkGroupName(request.REQUEST.get('name'), controller.group.name)
            form = GroupForm(initial={'experimenters':controller.experimenters}, data=request.POST.copy(), name_check=name_check)
            if form.is_valid():
                logger.debug("Update group form:" + str(form.cleaned_data))
                name = form.cleaned_data['name']
                description = form.cleaned_data['description']
                owners = form.cleaned_data['owners']
                permissions = form.cleaned_data['permissions']
                readonly = toBoolean(form.cleaned_data['readonly'])
                controller.updateGroup(name, owners, permissions, readonly, description)
                return HttpResponseRedirect(reverse("wagroups"))
            context = {'info':info, 'form':form, 'gid': gid}
    elif action == "update":
        template = "webadmin/group_edit.html"
        controller.containedExperimenters()
        form = ContainedExperimentersForm(initial={'members':controller.members, 'available':controller.available})
        if not form.is_valid():
            #available = form.cleaned_data['available']
            available = request.POST.getlist('available')
            #members = form.cleaned_data['members']
            members = request.POST.getlist('members')
            controller.setMembersOfGroup(available, members)
            return HttpResponseRedirect(reverse("wagroups"))
        context = {'info':info, 'form':form, 'controller': controller}
    elif action == "members":
        template = "webadmin/group_edit.html"
        controller.containedExperimenters()
        form = ContainedExperimentersForm(initial={'members':controller.members, 'available':controller.available})
        context = {'info':info, 'form':form, 'controller': controller}
    else:
        return HttpResponseRedirect(reverse("wagroups"))
    
    context['template'] = template
    return context


@login_required(isGroupOwner=True)
@render_response_admin()
def manage_group_owner(request, action, gid, conn=None, **kwargs):
    myaccount = True
    template = "webadmin/group_form_owner.html"
    
    info = {'myaccount':myaccount}
    
    controller = BaseGroup(conn, gid)
    
    if action == 'edit':
        permissions = controller.getActualPermissions()
        form = GroupOwnerForm(initial={'permissions': permissions, 'readonly': controller.isReadOnly()})
        context = {'info':info, 'form':form, 'gid': gid, 'permissions': permissions, 'group':controller.group, 'owners':controller.getOwnersNames()}
    elif action == "save":
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamyaccount", args=["edit", controller.group.id]))
        else:
            form = GroupOwnerForm(data=request.POST.copy())
            if form.is_valid():
                permissions = form.cleaned_data['permissions']
                readonly = toBoolean(form.cleaned_data['readonly'])
                controller.updatePermissions(permissions, readonly)
                return HttpResponseRedirect(reverse("wamyaccount"))
            context = {'info':info, 'form':form, 'gid': gid}
    else:
        return HttpResponseRedirect(reverse("wamyaccount"))
    
    context['template'] = template
    return context


@login_required(isAdmin=True)
@render_response_admin()
def ldap(request, conn=None, **kwargs):
    scripts = True
    template = "webadmin/ldap_search.html"
    
    info = {'scripts':scripts}

    controller = None
    
    context = {'info':info, 'controller':controller}
    context['template'] = template
    return context

@login_required(isAdmin=True)
def imports(request, **kwargs):
    return HttpResponseRedirect(reverse("waindex"))


@login_required()
@render_response_admin()
def my_account(request, action=None, conn=None, **kwargs):
    myaccount = True
    template = "webadmin/myaccount.html"
    
    info = {'myaccount':myaccount}
    
    myaccount = BaseExperimenter(conn)
    password_form = ChangePassword()
    myaccount.getMyDetails()
    myaccount.getOwnedGroups()
    
    form = None
    
    if action == "save":
        if request.method != 'POST':
            return HttpResponseRedirect(reverse(viewname="wamyaccount", args=["edit"]))
        else:
            email_check = conn.checkEmail(request.REQUEST.get('email'), myaccount.experimenter.email)
            form = MyAccountForm(data=request.POST.copy(), initial={'groups':myaccount.otherGroups}, email_check=email_check)
            if form.is_valid():
                firstName = form.cleaned_data['first_name']
                middleName = form.cleaned_data['middle_name']
                lastName = form.cleaned_data['last_name']
                email = form.cleaned_data['email']
                institution = form.cleaned_data['institution']
                defaultGroup = form.cleaned_data['default_group']
                myaccount.updateMyAccount(firstName, lastName, email, defaultGroup, middleName, institution)
                return HttpResponseRedirect(reverse("wamyaccount"))
    
    else:
        form = MyAccountForm(initial={'omename': myaccount.experimenter.omeName, 'first_name':myaccount.experimenter.firstName,
                                    'middle_name':myaccount.experimenter.middleName, 'last_name':myaccount.experimenter.lastName,
                                    'email':myaccount.experimenter.email, 'institution':myaccount.experimenter.institution,
                                    'default_group':myaccount.defaultGroup, 'groups':myaccount.otherGroups})
    
    photo_size = conn.getExperimenterPhotoSize()
    form = MyAccountForm(initial={'omename': myaccount.experimenter.omeName, 'first_name':myaccount.experimenter.firstName,
                                    'middle_name':myaccount.experimenter.middleName, 'last_name':myaccount.experimenter.lastName,
                                    'email':myaccount.experimenter.email, 'institution':myaccount.experimenter.institution,
                                    'default_group':myaccount.defaultGroup, 'groups':myaccount.otherGroups})
        
    context = {'info':info, 'form':form, 'ldapAuth': myaccount.ldapAuth, 'myaccount':myaccount, 'password_form':password_form}
    context['template'] = template
    return context


@login_required()
def myphoto(request, conn=None, **kwargs):
    photo = conn.getExperimenterPhoto()
    return HttpResponse(photo, mimetype='image/jpeg')


@login_required()
@render_response_admin()
def manage_avatar(request, action=None, conn=None, **kwargs):
    myaccount = True
    template = "webadmin/avatar.html"
    
    info = {'myaccount':myaccount}
    
    myaccount = BaseExperimenter(conn)
    myaccount.getMyDetails()
    myaccount.getOwnedGroups()
    
    edit_mode = False
    photo_size = None
    form_file = UploadPhotoForm()
    
    if action == "upload":
        if request.method == 'POST':
            form_file = UploadPhotoForm(request.POST, request.FILES)
            if form_file.is_valid():
                controller = BaseUploadFile(conn)
                controller.attach_photo(request.FILES['photo'])
                return HttpResponseRedirect(reverse(viewname="wamanageavatar", args=[conn.getEventContext().userId]))
    elif action == "crop": 
        x1 = long(request.REQUEST.get('x1'))
        x2 = long(request.REQUEST.get('x2'))
        y1 = long(request.REQUEST.get('y1'))
        y2 = long(request.REQUEST.get('y2'))
        box = (x1,y1,x2,y2)
        conn.cropExperimenterPhoto(box)
        return HttpResponseRedirect(reverse("wamyaccount"))
    elif action == "editphoto":
        photo_size = conn.getExperimenterPhotoSize()
        if photo_size is not None:
            edit_mode = True
    elif action == "deletephoto":
        conn.deleteExperimenterPhoto()
        return HttpResponseRedirect(reverse("wamyaccount"))
    
    photo_size = conn.getExperimenterPhotoSize()
    context = {'info':info, 'form_file':form_file, 'edit_mode':edit_mode, 'photo_size':photo_size, 'myaccount':myaccount}
    context['template'] = template
    return context


@login_required()
@render_response_admin()
def drivespace(request, conn=None, **kwargs):
    drivespace = True
    template = "webadmin/drivespace.html"
    
    info = {'drivespace':drivespace}
    controller = BaseDriveSpace(conn)
        
    context = {'info':info, 'driveSpace': {'free':controller.freeSpace, 'used':controller.usedSpace }}
    context['template'] = template
    return context


@login_required()
def load_drivespace(request, conn=None, **kwargs):
    offset = request.REQUEST.get('offset', 0)
    rv = usersData(conn, offset)
    return HttpResponse(simplejson.dumps(rv),mimetype='application/json')
