package com.dlsc.gemsfx;

import com.dlsc.gemsfx.skins.PhoneNumberFieldSkin;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.Callback;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;

public class PhoneNumberField extends Control {

    public static final String DEFAULT_STYLE_CLASS = "phone-number-field";
    public static final String DEFAULT_MASK = "(###) ###-##-##";

    private final TextField textField = new TextField();

    public PhoneNumberField() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        getAvailableCountryCodes().setAll(CountryCallingCode.Defaults.values());
        textField.setTextFormatter(new TextFormatter<>(new PhoneNumberFormatter()));
        textField.textProperty().bindBidirectional(phoneNumberProperty());
    }

    public final TextField getTextField() {
        return textField;
    }

    @Override
    public String getUserAgentStylesheet() {
        return Objects.requireNonNull(PhoneNumberField.class.getResource("phone-number-field.css")).toExternalForm();
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new PhoneNumberFieldSkin(this, countryCode);
    }

    // VALUES
    private final StringProperty phoneNumber = new SimpleStringProperty(this, "phoneNumber") {
        private boolean selfUpdate;
        @Override
        public void set(String newPhone) {
            if (selfUpdate) {
                return;
            }

            try {
                selfUpdate = true;

                // Set the value first, so that the binding will be triggered
                super.set(newPhone);

                if (isForceLocalPhoneNumber()) {
                    // No need to infer the country code, just use the local phone number
                    setCountryCode(null);
                    setLocalPhoneNumber(newPhone);
                    return;
                }

                // Update country code and local phone
                Pair<CountryCallingCode, String> parsedNumber = PhoneNumberParser.parse(newPhone, getAvailableCountryCodes());
                if (parsedNumber != null) {
                    setCountryCode(parsedNumber.getKey());
                    setLocalPhoneNumber(parsedNumber.getValue());
                } else {
                    setCountryCode(null);
                    setLocalPhoneNumber(null);
                }
            }
            finally {
                selfUpdate = false;
            }
        }
    };

    public final StringProperty phoneNumberProperty() {
        return phoneNumber;
    }

    public final String getPhoneNumber() {
        return phoneNumberProperty().get();
    }

    public final void setPhoneNumber(String phoneNumber) {
        phoneNumberProperty().set(phoneNumber);
    }

    private final ReadOnlyStringWrapper localPhoneNumber = new ReadOnlyStringWrapper(this, "localPhoneNumber");

    public final ReadOnlyStringProperty localPhoneNumberProperty() {
        return localPhoneNumber.getReadOnlyProperty();
    }

    public final String getLocalPhoneNumber() {
        return localPhoneNumber.get();
    }

    private void setLocalPhoneNumber(String localPhoneNumber) {
        this.localPhoneNumber.set(localPhoneNumber);
    }

    private final ReadOnlyObjectWrapper<CountryCallingCode> countryCode = new ReadOnlyObjectWrapper<>(this, "countryCode") {
        private boolean selfUpdate;
        @Override
        public void set(CountryCallingCode countryCallingCode) {
            if (selfUpdate) {
                return;
            }
            try {
                selfUpdate = true;

                // Set the value first, so that the binding will be triggered
                super.set(countryCallingCode);

                // For now replace the entire text, it might be good to preserve the local number and just change the country code
                setPhoneNumber(Optional.ofNullable(countryCallingCode)
                    .map(CountryCallingCode::defaultPhonePrefix)
                    .orElse(null));

                Optional.ofNullable(getMaskProvider()).ifPresent(m -> setMask(m.call(countryCallingCode)));

            } finally {
                selfUpdate = false;
            }
        }
    };

    public final ReadOnlyObjectProperty<CountryCallingCode> countryCodeProperty() {
        return countryCode.getReadOnlyProperty();
    }

    public final CountryCallingCode getCountryCode() {
        return countryCode.get();
    }

    private void setCountryCode(CountryCallingCode countryCode) {
        this.countryCode.set(countryCode);
    }

    // SETTINGS

    private final ObservableList<CountryCallingCode> availableCountryCodes = FXCollections.observableArrayList();

    public final ObservableList<CountryCallingCode> getAvailableCountryCodes() {
        return availableCountryCodes;
    }

    private final ObservableList<CountryCallingCode> preferredCountryCodes = FXCollections.observableArrayList();

    public final ObservableList<CountryCallingCode> getPreferredCountryCodes() {
        return preferredCountryCodes;
    }

    private final ObjectProperty<CountryCallingCode> defaultCountryCode = new SimpleObjectProperty<>(this, "defaultCountryCode") {
        @Override
        public void set(CountryCallingCode countryCallingCode) {
            super.set(countryCallingCode);
            if (getCountryCode() == null) {
                setCountryCode(countryCallingCode);
            }
        }
    };

    public final ObjectProperty<CountryCallingCode> defaultCountryCodeProperty() {
        return defaultCountryCode;
    }

    public final CountryCallingCode getDefaultCountryCode() {
        return defaultCountryCodeProperty().get();
    }

    public final void setDefaultCountryCode(CountryCallingCode defaultCountryCode) {
        defaultCountryCodeProperty().set(defaultCountryCode);
    }

    private final ObjectProperty<CountryCallingCode> fixedCountryCode = new SimpleObjectProperty<>(this, "fixedCountryCode") {
        @Override
        public void set(CountryCallingCode countryCallingCode) {
            super.set(countryCallingCode);
            if (countryCallingCode != null && !isForceLocalPhoneNumber()) {
                setCountryCode(countryCallingCode);
            }
        }
    };

    public final ObjectProperty<CountryCallingCode> fixedCountryCodeProperty() {
        return fixedCountryCode;
    }

    public final CountryCallingCode getFixedCountryCode() {
        return fixedCountryCodeProperty().get();
    }

    public final void setFixedCountryCode(CountryCallingCode fixedCountryCode) {
        fixedCountryCodeProperty().set(fixedCountryCode);
    }

    private final BooleanProperty forceLocalPhoneNumber = new SimpleBooleanProperty(this, "forceLocalPhoneNumber") {
        private CountryCallingCode lastCountryCode;
        @Override
        public void set(boolean forceLocal) {
            super.set(forceLocal);

            final String localPhone = Optional.ofNullable(getLocalPhoneNumber()).orElse("");

            if (forceLocal) {
                lastCountryCode = getCountryCode();
                setCountryCode(null);
                setPhoneNumber(localPhone);
            } else if (getFixedCountryCode() != null) {
                setPhoneNumber(getFixedCountryCode().defaultPhonePrefix() + localPhone);
            } else if (lastCountryCode != null) {
                setPhoneNumber(lastCountryCode.defaultPhonePrefix() + localPhone);
            } else {
                setPhoneNumber(localPhone);
            }
        }
    };

    public final BooleanProperty forceLocalPhoneNumberProperty() {
        return forceLocalPhoneNumber;
    }

    public final boolean isForceLocalPhoneNumber() {
        return forceLocalPhoneNumberProperty().get();
    }

    public final void setForceLocalPhoneNumber(boolean forceLocalPhoneNumber) {
        forceLocalPhoneNumberProperty().set(forceLocalPhoneNumber);
    }

    private final ObjectProperty<Callback<CountryCallingCode, String>> maskProvider = new SimpleObjectProperty<>(this, "maskProvider", code ->
        Optional.ofNullable(code).map(CountryCallingCode::mask).orElse(null)) {
        @Override
        public void set(Callback<CountryCallingCode, String> newMaskProvider) {
            super.set(newMaskProvider);
            setMask(Optional.ofNullable(newMaskProvider).map(p -> p.call(getCountryCode())).orElse(null));
        }
    };

    public final ObjectProperty<Callback<CountryCallingCode, String>> maskProviderProperty() {
        return maskProvider;
    }

    public final Callback<CountryCallingCode, String> getMaskProvider() {
        return maskProviderProperty().get();
    }

    public final void setMaskProvider(Callback<CountryCallingCode, String> mask) {
        maskProviderProperty().set(mask);
    }

    private final ReadOnlyStringWrapper mask = new ReadOnlyStringWrapper(this, "mask");

    public final ReadOnlyStringProperty maskProperty() {
        return mask.getReadOnlyProperty();
    }

    public final String getMask() {
        return mask.get();
    }

    private void setMask(String mask) {
        this.mask.set(mask);
    }

    private final ObjectProperty<Callback<CountryCallingCode, Node>> countryCodeViewFactory = new SimpleObjectProperty<>(this, "countryCodeViewFactory");

    public final ObjectProperty<Callback<CountryCallingCode, Node>> countryCodeViewFactoryProperty() {
        return countryCodeViewFactory;
    }

    public final Callback<CountryCallingCode, Node> getCountryCodeViewFactory() {
        return countryCodeViewFactoryProperty().get();
    }

    public final void setCountryCodeViewFactory(Callback<CountryCallingCode, Node> countryCodeViewFactory) {
        countryCodeViewFactoryProperty().set(countryCodeViewFactory);
    }

    public interface CountryCallingCode {

        int countryCode();

        int[] areaCodes();

        String iso2Code();

        String mask();

        default Integer defaultAreaCode() {
            return areaCodes().length > 0 ? areaCodes()[0] : null;
        }

        default String defaultPhonePrefix() {
            StringBuilder value = new StringBuilder();
            value.append(countryCode());
            Integer defaultAreaCode = defaultAreaCode();
            if (defaultAreaCode != null) {
                value.append(defaultAreaCode);
            }
            return value.toString();
        }

        enum Defaults implements CountryCallingCode {

            AFGHANISTAN(93, "AF"),
            ALAND_ISLANDS(358, "AX", 18),
            ALBANIA(355, "AL"),
            ALGERIA(213, "DZ"),
            AMERICAN_SAMOA(1, "AS", 684),
            ANDORRA(376, "AD"),
            ANGOLA(244, "AO"),
            ANGUILLA(1, "AI", 264),
            ANTIGUA_AND_BARBUDA(1, "AG", 268),
            ARGENTINA(54, "AR"),
            ARMENIA(374, "AM"),
            ARUBA(297, "AW"),
            AUSTRALIA(61, "AU"),
            AUSTRALIA_ANTARCTIC_TERRITORIES(672, "AQ", 1),
            AUSTRIA(43, "AT"),
            AZERBAIJAN(994, "AZ"),
            BAHAMAS(1, "BS", 242),
            BAHRAIN(973, "BH"),
            BANGLADESH(880, "BD"),
            BARBADOS(1, "BB", 246),
            BELARUS(375, "BY"),
            BELGIUM(32, "BE"),
            BELIZE(501, "BZ"),
            BENIN(229, "BJ"),
            BERMUDA(1, "BM", 441),
            BHUTAN(975, "BT"),
            BOLIVIA(591, "BO"),
            BONAIRE(599, "BQ", 7),
            BOSNIA_AND_HERZEGOVINA(387, "BA"),
            BOTSWANA(267, "BW"),
            BRAZIL(55, "BR"),
            BRITISH_INDIAN_OCEAN_TERRITORY(246, "IO"),
            BRITISH_VIRGIN_ISLANDS(1, "VG", 284),
            BRUNEI(673, "BN"),
            BULGARIA(359, "BG"),
            BURKINA_FASO(226, "BF"),
            BURUNDI(257, "BI"),
            CAMBODIA(855, "KH"),
            CAMEROON(237, "CM"),
            CANADA(1, "CA"),
            CAPE_VERDE(238, "CV"),
            CAYMAN_ISLANDS(1, "KY", 345),
            CENTRAL_AFRICAN_REPUBLIC(236, "CF"),
            CHAD(235, "TD"),
            CHILE(56, "CL"),
            CHINA(86, "CN"),
            CHRISTMAS_ISLAND(61, "CX", 89164),
            COCOS_ISLANDS(61, "CC", 89162),
            COLOMBIA(57, "CO"),
            COMOROS(269, "KM"),
            CONGO(242, "CG"),
            COOK_ISLANDS(682, "CK"),
            COSTA_RICA(506, "CR"),
            CROATIA(385, "HR"),
            CUBA(53, "CU"),
            CURACAO(599, "CW", 9),
            CYPRUS(357, "CY"),
            CZECH_REPUBLIC(420, "CZ"),
            DEMOCRATIC_REPUBLIC_OF_THE_CONGO(243, "CD"),
            DENMARK(45, "DK"),
            DJIBOUTI(253, "DJ"),
            DOMINICA(1, "DM", 767),
            DOMINICAN_REPUBLIC(1, "DO", 809, 829, 849),
            EAST_TIMOR(670, "TL"),
            ECUADOR(593, "EC"),
            EGYPT(20, "EG"),
            EL_SALVADOR(503, "SV"),
            EQUATORIAL_GUINEA(240, "GQ"),
            ERITREA(291, "ER"),
            ESTONIA(372, "EE"),
            ETHIOPIA(251, "ET"),
            FALKLAND_ISLANDS(500, "FK"),
            FAROE_ISLANDS(298, "FO"),
            FIJI(679, "FJ"),
            FINLAND(358, "FI"),
            FRANCE(33, "FR"),
            FRENCH_GUIANA(594, "GF"),
            FRENCH_POLYNESIA(689, "PF"),
            GABON(241, "GA"),
            GAMBIA(220, "GM"),
            GEORGIA(995, "GE"),
            GERMANY(49, "DE"),
            GHANA(233, "GH"),
            GIBRALTAR(350, "GI"),
            GREECE(30, "GR"),
            GREENLAND(299, "GL"),
            GRENADA(1, "GD", 473),
            GUADELOUPE(590, "GP"),
            GUAM(1, "GU", 671),
            GUATEMALA(502, "GT"),
            GUERNSEY(44, "GG", 1481, 7781, 7839, 7911),
            GUINEA(224, "GN"),
            GUINEA_BISSAU(245, "GW"),
            GUYANA(592, "GY"),
            HAITI(509, "HT"),
            HONDURAS(504, "HN"),
            HONG_KONG(852, "HK"),
            HUNGARY(36, "HU"),
            ICELAND(354, "IS"),
            INDIA(91, "IN"),
            INDONESIA(62, "ID"),
            IRAN(98, "IR"),
            IRAQ(964, "IQ"),
            IRELAND(353, "IE"),
            ISLE_OF_MAN(44, "IM", 1624, 7524, 7624, 7924),
            ISRAEL(972, "IL"),
            ITALY(39, "IT"),
            IVORY_COAST(225, "CI"),
            JAMAICA(1, "JM", 658, 876),
            JAN_MAYEN(47, "SJ", 79),
            JAPAN(81, "JP"),
            JERSEY(44, "JE", 1534),
            JORDAN(962, "JO"),
            KAZAKHSTAN(7, "KZ", 6, 7),
            KENYA(254, "KE"),
            KIRIBATI(686, "KI"),
            KOREA_NORTH(850, "KP"),
            KOREA_SOUTH(82, "KR"),
            KOSOVO(383, "XK"),
            KUWAIT(965, "KW"),
            KYRGYZSTAN(996, "KG"),
            LAOS(856, "LA"),
            LATVIA(371, "LV"),
            LEBANON(961, "LB"),
            LESOTHO(266, "LS"),
            LIBERIA(231, "LR"),
            LIBYA(218, "LY"),
            LIECHTENSTEIN(423, "LI"),
            LITHUANIA(370, "LT"),
            LUXEMBOURG(352, "LU"),
            MACAU(853, "MO"),
            MACEDONIA(389, "MK"),
            MADAGASCAR(261, "MG"),
            MALAWI(265, "MW"),
            MALAYSIA(60, "MY"),
            MALDIVES(960, "MV"),
            MALI(223, "ML"),
            MALTA(356, "MT"),
            MARSHALL_ISLANDS(692, "MH"),
            MARTINIQUE(596, "MQ"),
            MAURITANIA(222, "MR"),
            MAURITIUS(230, "MU"),
            MAYOTTE(262, "YT", 269, 639),
            MEXICO(52, "MX"),
            MICRONESIA(691, "FM"),
            MOLDOVA(373, "MD"),
            MONACO(377, "MC"),
            MONGOLIA(976, "MN"),
            MONTENEGRO(382, "ME"),
            MONTSERRAT(1, "MS", 664),
            MOROCCO(212, "MA"),
            MOZAMBIQUE(258, "MZ"),
            MYANMAR(95, "MM"),
            NAMIBIA(264, "NA"),
            NAURU(674, "NR"),
            NEPAL(977, "NP"),
            NETHERLANDS(31, "NL"),
            NEW_CALEDONIA(687, "NC"),
            NEW_ZEALAND(64, "NZ"),
            NICARAGUA(505, "NI"),
            NIGER(227, "NE"),
            NIGERIA(234, "NG"),
            NIUE(683, "NU"),
            NORFOLK_ISLAND(672, "NF", 3),
            NORTHERN_MARIANA_ISLANDS(1, "MP", 670),
            NORWAY(47, "NO"),
            OMAN(968, "OM"),
            PAKISTAN(92, "PK"),
            PALAU(680, "PW"),
            PALESTINE(970, "PS"),
            PANAMA(507, "PA"),
            PAPUA_NEW_GUINEA(675, "PG"),
            PARAGUAY(595, "PY"),
            PERU(51, "PE"),
            PHILIPPINES(63, "PH"),
            POLAND(48, "PL"),
            PORTUGAL(351, "PT"),
            PUERTO_RICO(1, "PR", 787, 930),
            QATAR(974, "QA"),
            REUNION(262, "RE"),
            ROMANIA(40, "RO"),
            RUSSIA(7, "RU"),
            RWANDA(250, "RW"),
            SAINT_HELENA(290, "SH"),
            SAINT_KITTS_AND_NEVIS(1, "KN", 869),
            SAINT_LUCIA(1, "LC", 758),
            SAINT_PIERRE_AND_MIQUELON(508, "PM"),
            SAINT_VINCENT_AND_THE_GRENADINES(1, "VC", 784),
            SAMOA(685, "WS"),
            SAN_MARINO(378, "SM"),
            SAO_TOME_AND_PRINCIPE(239, "ST"),
            SAUDI_ARABIA(966, "SA"),
            SENEGAL(221, "SN"),
            SERBIA(381, "RS"),
            SEYCHELLES(248, "SC"),
            SIERRA_LEONE(232, "SL"),
            SINGAPORE(65, "SG"),
            SLOVAKIA(421, "SK"),
            SLOVENIA(386, "SI"),
            SOLOMON_ISLANDS(677, "SB"),
            SOMALIA(252, "SO"),
            SOUTH_AFRICA(27, "ZA"),
            SOUTH_SUDAN(211, "SS"),
            SPAIN(34, "ES"),
            SRI_LANKA(94, "LK"),
            SUDAN(249, "SD"),
            SURINAME(597, "SR"),
            SVALBARD_AND_JAN_MAYEN(47, "SJ"),
            SWAZILAND(268, "SZ"),
            SWEDEN(46, "SE"),
            SWITZERLAND(41, "CH"),
            SYRIA(963, "SY"),
            TAIWAN(886, "TW"),
            TAJIKISTAN(992, "TJ"),
            TANZANIA(255, "TZ"),
            THAILAND(66, "TH"),
            TOGO(228, "TG"),
            TOKELAU(690, "TK"),
            TONGA(676, "TO"),
            TRINIDAD_AND_TOBAGO(1, "TT", 868),
            TUNISIA(216, "TN"),
            TURKEY(90, "TR"),
            TURKMENISTAN(993, "TM"),
            TURKS_AND_CAICOS_ISLANDS(1, "TC", 649),
            TUVALU(688, "TV"),
            UGANDA(256, "UG"),
            UKRAINE(380, "UA"),
            UNITED_ARAB_EMIRATES(971, "AE"),
            UNITED_KINGDOM(44, "GB"),
            UNITED_STATES(1, "US"),
            URUGUAY(598, "UY"),
            UZBEKISTAN(998, "UZ"),
            VANUATU(678, "VU"),
            VATICAN_CITY(379, "VA"),
            VENEZUELA(58, "VE"),
            VIETNAM(84, "VN"),
            VIRGIN_ISLANDS(1, "VI", 340),
            WALLIS_AND_FUTUNA(681, "WF"),
            WESTERN_SAHARA(212, "EH"),
            YEMEN(967, "YE"),
            ZAMBIA(260, "ZM"),
            ZANZIBAR(255, "TZ"),
            ZIMBABWE(263, "ZW")
            ;

            private final int code;
            private final String iso2Code;
            private final int[] areaCodes;
            private final String mask;

            Defaults(int code, String iso2Code, int... areaCodes) {
                this(code, iso2Code, DEFAULT_MASK, areaCodes);
            }

            Defaults(int code, String iso2Code, String mask, int... areaCodes) {
                this.code = code;
                this.iso2Code = iso2Code;
                this.mask = mask;
                this.areaCodes = Optional.ofNullable(areaCodes).orElse(new int[0]);
            }

            @Override
            public int countryCode() {
                return code;
            }

            @Override
            public int[] areaCodes() {
                return areaCodes;
            }

            @Override
            public String iso2Code() {
                return iso2Code;
            }

            @Override
            public String mask() {
                return mask;
            }

        }

    }

    private static final class PhoneNumberParser {

        static Pair<CountryCallingCode, String> parse(String phoneNumber, Collection<CountryCallingCode> availableCountryCodes) {
            Map<CountryCallingCode, String> localNumbers = new HashMap<>();
            TreeMap<Integer, List<CountryCallingCode>> scores = new TreeMap<>();

            for (CountryCallingCode code : availableCountryCodes) {
                Pair<Integer, String> score = scoreAndLocalNumber(code, phoneNumber);
                if (score.getKey() > 0) {
                    scores.computeIfAbsent(score.getKey(), s -> new ArrayList<>()).add(code);
                    localNumbers.put(code, score.getValue());
                }
            }

            return inferBestMatch(scores.lastEntry(), localNumbers);
        }

        private static Pair<CountryCallingCode, String> inferBestMatch(
            Map.Entry<Integer, List<CountryCallingCode>> highestScore,
            Map<CountryCallingCode, String> localNumbers) {

            if (highestScore == null) {
                return null;
            }

            List<CountryCallingCode> matchingCodes = highestScore.getValue();

            CountryCallingCode code;
            if (matchingCodes.size() > 1) {
                // Here there will be some ambiguity since two countries have same score
                // TODO we need some sort of logic here, for now using the last one.
                code = matchingCodes.get(matchingCodes.size() - 1);
            } else {
                code = matchingCodes.get(0);
            }

            return new Pair<>(code, localNumbers.get(code));
        }

        private static Pair<Integer, String> scoreAndLocalNumber(CountryCallingCode code, String phoneNumber) {
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                String countryPrefix = String.valueOf(code.countryCode());

                if (code.areaCodes().length == 0) {
                    if (phoneNumber.startsWith(countryPrefix)) {
                        String localNumber = phoneNumber.length() > countryPrefix.length() ?
                            phoneNumber.substring(countryPrefix.length()) :
                            null;
                        return new Pair<>(1, localNumber);
                    }
                } else {
                    for (int areaCode : code.areaCodes()) {
                        String areaCodePrefix = countryPrefix + areaCode;
                        if (phoneNumber.startsWith(areaCodePrefix)) {
                            String localNumber = phoneNumber.length() > areaCodePrefix.length() ?
                                phoneNumber.substring(areaCodePrefix.length()) :
                                phoneNumber.substring(countryPrefix.length());
                            return new Pair<>(2, localNumber);
                        }
                    }
                }
            }

            return new Pair<>(0, null);
        }

    }

    private final class PhoneNumberFormatter implements UnaryOperator<TextFormatter.Change> {

        private PhoneNumberFormatter() {
            maskProperty().addListener((observable, oldMask, newMask) -> {

            });
        }

        @Override
        public TextFormatter.Change apply(TextFormatter.Change change) {
            if (change.isAdded() || change.isReplaced()) {
                String text = change.getText();
                if (!text.matches("[0-9]+")) {
                    return null;
                }

                // TODO Apply the mask here

            } else if (change.isDeleted()) {
                if (getFixedCountryCode() != null && !isForceLocalPhoneNumber()) {
                    String newText = change.getControlNewText();
                    if (!newText.startsWith(getFixedCountryCode().defaultPhonePrefix())) {
                        return null;
                    }
                }
            }

            return change;
        }
    }

}


