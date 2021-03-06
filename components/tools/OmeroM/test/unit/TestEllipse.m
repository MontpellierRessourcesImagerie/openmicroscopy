% Unit tests for the Ellipse shape helper function
%
% Require MATLAB xUnit Test Framework to be installed
% http://www.mathworks.com/matlabcentral/fileexchange/22846-matlab-xunit-test-framework

% Copyright (C) 2013-2016 University of Dundee & Open Microscopy Environment.
% All rights reserved.
%
% This program is free software; you can redistribute it and/or modify
% it under the terms of the GNU General Public License as published by
% the Free Software Foundation; either version 2 of the License, or
% (at your option) any later version.
%
% This program is distributed in the hope that it will be useful,
% but WITHOUT ANY WARRANTY; without even the implied warranty of
% MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
% GNU General Public License for more details.
%
% You should have received a copy of the GNU General Public License along
% with this program; if not, write to the Free Software Foundation, Inc.,
% 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

classdef TestEllipse < TestShape
    
    properties
        x = 10
        y = 20
        radiusx = 5
        radiusy = 8
    end
    
    methods
        function self = TestEllipse(name)
            self = self@TestShape(name);
        end
        
        function setUp(self)
            self.createEllipse()
        end
        
        function createEllipse(self, iscircle)
            if nargin<2 || ~iscircle
                self.shape = createEllipse(self.x, self.y, self.radiusx, self.radiusy);
            else
                self.shape = createEllipse(self.x, self.y, self.radiusx);
            end
        end
        
        function testSimpleEllipse(self)
            assertTrue(isa(self.shape, 'omero.model.EllipseI'));
            assertEqual(self.shape.getX().getValue(), self.x);
            assertEqual(self.shape.getY().getValue(), self.y);
            assertEqual(self.shape.getRadiusX().getValue(), self.radiusx);
            assertEqual(self.shape.getRadiusY().getValue(), self.radiusy);
        end
        
        function testCircle(self)
            self.createEllipse(true);
            
            assertTrue(isa(self.shape, 'omero.model.EllipseI'));
            assertEqual(self.shape.getX().getValue(), self.x);
            assertEqual(self.shape.getY().getValue(), self.y);
            assertEqual(self.shape.getRadiusX().getValue(), self.radiusx);
            assertEqual(self.shape.getRadiusY().getValue(), self.radiusx);
        end
        
        function testNegativeRadiusX(self)
            self.radiusx = -1;
            assertExceptionThrown(@() self.createEllipse(),...
                'MATLAB:InputParser:ArgumentFailedValidation');
        end
        
        function testNegativeRadiusY(self)
            self.radiusy = -1;
            assertExceptionThrown(@() self.createEllipse(),...
                'MATLAB:InputParser:ArgumentFailedValidation');
        end
        
        function testZeroRadiusX(self)
            self.radiusx = 0;
            assertExceptionThrown(@() self.createEllipse(),...
                'MATLAB:InputParser:ArgumentFailedValidation');
        end
        
        function testZeroRadiusY(self)
            self.radiusy = 0;
            assertExceptionThrown(@() self.createEllipse(),...
                'MATLAB:InputParser:ArgumentFailedValidation');
        end
    end
end
