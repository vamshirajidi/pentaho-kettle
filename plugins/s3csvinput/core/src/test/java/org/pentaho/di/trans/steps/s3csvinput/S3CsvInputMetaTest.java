/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.s3csvinput;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.di.core.Const;
import org.pentaho.di.core.encryption.Encr;
import org.pentaho.di.core.encryption.TwoWayPasswordEncoderPluginType;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.core.util.EnvUtil;
import org.pentaho.di.trans.steps.loadsave.LoadSaveTester;
import org.pentaho.di.trans.steps.loadsave.validator.ArrayLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.FieldLoadSaveValidator;
import org.pentaho.di.trans.steps.loadsave.validator.TextFileInputFieldValidator;
import org.pentaho.di.trans.steps.textfileinput.TextFileInputField;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * @author Tatsiana_Kasiankova
 *
 */
@SuppressWarnings( "deprecation" )
@RunWith( MockitoJUnitRunner.StrictStubs.class )
public class S3CsvInputMetaTest {

  private static final String TEST_AWS_SECRET_KEY = "TestAwsSecretKey";
  private static final String TEST_ACCESS_KEY = "TestAccessKey";
  private static final String TEST_AWS_SECRET_KEY_ENCRYPTED = "Encrypted 2eafddcbc2bd081b7ae1abc75cab9aac3";
  private static final String TEST_ACCESS_KEY_ENCRYPTED = "Encrypted 2be98af9c0fd486a5a81aab63cdb9aac3";
  private MockedStatic<EnvUtil> mockEnvUtil;

  @BeforeClass
  public static void once() throws KettleException {
    PluginRegistry.addPluginType( TwoWayPasswordEncoderPluginType.getInstance() );
    PluginRegistry.init( true );
    String passwordEncoderPluginID =
      Const.NVL( EnvUtil.getSystemProperty( Const.KETTLE_PASSWORD_ENCODER_PLUGIN ), "Kettle" );
    Encr.init( passwordEncoderPluginID );
  }

  @Before
  public void setup() {
    mockEnvUtil = mockStatic( EnvUtil.class );
  }

  @After
  public void tearDown() {
    mockEnvUtil.close();
  }

  @Test
  public void testSerialization() throws KettleException {
    List<String> attributes = Arrays.asList( "AwsAccessKey", "AwsSecretKey", "Bucket", "Filename", "FilenameField",
      "RowNumField", "IncludingFilename", "Delimiter", "Enclosure", "HeaderPresent", "MaxLineSize",
      "LazyConversionActive", "RunningInParallel", "InputFields" );

    Map<String, FieldLoadSaveValidator<?>> typeMap = new HashMap<>();
    typeMap.put( TextFileInputField[].class.getCanonicalName(),
      new ArrayLoadSaveValidator<>( new TextFileInputFieldValidator() ) );
    Map<String, String> getterMap = new HashMap<>();
    Map<String, String> setterMap = new HashMap<>();
    LoadSaveTester<S3CsvInputMeta> tester = new LoadSaveTester<>( S3CsvInputMeta.class, attributes,
      getterMap, setterMap, new HashMap<String, FieldLoadSaveValidator<?>>(), typeMap );
    tester.testSerialization();
  }

  @Test
  public void getUseAwsDefaultCredentialsWithoutCredentials() {
    S3CsvInputMeta meta = new S3CsvInputMeta();
    assertTrue( meta.getUseAwsDefaultCredentials() );
  }

  @Test
  public void getUseAwsDefaultCredentialsWithCredentials() {
    S3CsvInputMeta meta = new S3CsvInputMeta();
    meta.setAwsAccessKey( TEST_ACCESS_KEY_ENCRYPTED );
    meta.setAwsSecretKey( TEST_AWS_SECRET_KEY_ENCRYPTED );

    assertFalse( meta.getUseAwsDefaultCredentials() );
  }

  @Test
  public void getUseAwsDefaultCredentialsOverrideCredentials() {
    S3CsvInputMeta meta = new S3CsvInputMeta();
    meta.setAwsAccessKey( TEST_ACCESS_KEY_ENCRYPTED );
    meta.setAwsSecretKey( TEST_AWS_SECRET_KEY_ENCRYPTED );

    when( EnvUtil.getSystemProperty( Const.KETTLE_USE_AWS_DEFAULT_CREDENTIALS ) ).thenReturn( "Y" );
    assertTrue( meta.getUseAwsDefaultCredentials() );
  }
}
