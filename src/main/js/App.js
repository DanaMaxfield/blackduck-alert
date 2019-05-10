import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { withRouter } from 'react-router-dom';

import '@fortawesome/fontawesome-free/scss/fontawesome.scss';
import '@fortawesome/fontawesome-free/scss/v4-shims.scss';
import '@fortawesome/fontawesome-free/js/all.js';
import '@fortawesome/fontawesome-free/js/v4-shims.js';

import MainPage from 'MainPage';
import LoginPage from 'LoginPage';
import AboutInfoFooter from 'component/AboutInfoFooter';
import SetupPage from 'SetupPage';
import { verifyLogin } from 'store/actions/session';
import { getInitialSystemSetup } from 'store/actions/system';
import { getDescriptors } from 'store/actions/descriptors';

import LogoutPage from 'LogoutPage';
import '../css/main.scss';


class App extends Component {
    componentDidMount() {
        this.props.getDescriptors();
        this.props.verifyLogin();
        this.props.getSettings();
    }

    render() {
        if (this.props.initializing) {
            return (<div />);
        }

        if (this.props.logoutPerformed) {
            return <LogoutPage />;
        }

        let contentPage = (this.props.loggedIn) ? <MainPage /> : <LoginPage />;
        if (!this.props.systemInitialized) {
            contentPage = <SetupPage />;
        }

        return (
            <div>
                {contentPage}
                <AboutInfoFooter />
            </div>
        );
    }
}

App.propTypes = {
    loggedIn: PropTypes.bool.isRequired,
    logoutPerformed: PropTypes.bool.isRequired,
    initializing: PropTypes.bool.isRequired,
    verifyLogin: PropTypes.func.isRequired,
    getSettings: PropTypes.func.isRequired,
    systemInitialized: PropTypes.bool.isRequired,
    getDescriptors: PropTypes.func.isRequired
};

// Redux mappings to be used later....
const mapStateToProps = state => ({
    loggedIn: state.session.loggedIn,
    logoutPerformed: state.session.logoutPerformed,
    initializing: state.session.initializing,
    systemInitialized: state.system.systemInitialized
});

const mapDispatchToProps = dispatch => ({
    verifyLogin: () => dispatch(verifyLogin()),
    getSettings: () => dispatch(getInitialSystemSetup()),
    getDescriptors: () => dispatch(getDescriptors())
});

export default withRouter(connect(mapStateToProps, mapDispatchToProps)(App));
